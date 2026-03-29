package Fork::HTTP;

use Moo;
use Try::Tiny;
use DBIx::Connector;
use Router::Simple;
use Plack::Request;

has dbic => (
    is      => 'lazy',
    default => sub {
        my $self = shift;
        my $dsn  = "dbi:SQLite:fork.db";
        my $dbh  = DBIx::Connector->new( $dsn, '', '', { RaiseError => 1 } );
        return $dbh;
    },
);

has router => (
    is      => 'lazy',
    default => sub {
        Router::Simple->new;
    }
);

has json => (
    is      => 'lazy',
    default => sub {
        require JSON::PP;
        JSON::PP->new->utf8->canonical;
    }
);

sub _init_router {
    my ($self) = @_;
    my $router = $self->router;

    $router->connect(
        '/',
        { handler => 'Forum', action => 'index' },
        { method  => 'GET' }
    );
    $router->connect(
        '/.well-known/webfinger',
        { handler => 'API::WellKnown', action => 'webfinger' },
        { method  => 'GET' }
    );
    $router->connect(
        '/f',
        { handler => 'Forum', action => 'join_forum' },
        { method  => 'POST' }
    );
    $router->connect(
        '/f/:forum',
        { handler => 'Forum', action => 'forum' },
        { method  => 'GET' }
    );
    $router->connect(
        '/actor/:actor_id',
        { handler => 'API::Actor', action => 'get_actor' },
        { method  => 'GET' }
    );
}

sub to_app {
    my ($self) = @_;
    $self->_init_router;
    return sub {
        my ($env) = @_;
        my $req   = Plack::Request->new($env);
        my $match = $self->router->match($env);
        if ($match) {
            my $handler_name = "Fork::HTTP::Handler::$match->{handler}";

            ( my $handler_file = "${handler_name}.pm" ) =~ s{::}{/}g;
            my $loaded = eval {
                require $handler_file;
                1;
            };

            if ( !$loaded ) {
                warn "Failed to load handler $handler_name: $@";
                return [
                    500,
                    [ 'Content-Type' => 'text/plain' ],
                    ['Internal Server Error']
                ];
            }

            my $handler = $handler_name->new(
                _json => $self->json,
                dbh   => $self->dbic->dbh,
                req   => $req,
                res   => $req->new_response(200),
                env   => $env
            );

            my $action = $match->{action};

            my $args;
            for ( keys %$match ) {
                next if $_ eq 'handler' || $_ eq 'action';
                $args->{$_} = $match->{$_};
            }

            if ( $handler->can($action) ) {
                $handler->$action($args);

                my $res = $handler->res;

                if ( ref($res) eq 'Plack::Response' ) {
                    return $res->finalize;
                }
                elsif ( ( ref($res) eq 'HASH' || ref($res) eq 'ARRAY' )
                    && exists $res->{status} )
                {
                    return [
                        $res->{status},
                        $res->{headers}
                          || [
                            'Content-Type' => 'application/json; charset=utf-8'
                          ],
                        [ $self->json->encode($res) ]
                    ];
                }

                warn
"Handler $handler_name action '$action' returned invalid response";
                return $req->new_response( 500,
                    [ 'Content-Type' => 'text/plain' ],
                    ['Internal Server Error'] )->finalize;
            }

            warn "Handler $handler_name does not have action '$action'";
            return $req->new_response( 500, [ 'Content-Type' => 'text/plain' ],
                ['Internal Server Error'] )->finalize;

        }

        return $req->new_response( 404, [ 'Content-Type' => 'text/plain' ],
            ['Not Found'] )->finalize;

    }
}

1;
