package Fork::HTTP::Handler::API::WellKnown;

use Moo;

use Fork::Service::Forum;

with 'Fork::HTTP::Handler';

has forum_service => (
    is      => 'lazy',
    default => sub {
        Fork::Service::Forum->new( dbh => shift->dbh );
    }
);

sub webfinger {
    my ($self) = @_;

    my $query = $self->req->query_parameters;
    if ( !defined $query->get('resource') ) {
        return $self->error( 400, 'Missing resource parameter' );
    }

    my $resource = $query->get('resource');

    if ( $resource !~ /^forum:[a-zA-Z]+$/ ) {
        return $self->error( 400, 'Invalid resource type' );
    }

    my ( undef, $forum ) = split /:/, $resource, 2;
    if ( !$self->forum_service->get_forum($forum) ) {
        return $self->error( 404, 'Not Found' );
    }

    my $base_url = $self->req->base;

    $base_url .= '/' unless $base_url =~ /\/$/;

    $self->json(
        +{
            subject => $resource,
            links   => [
                {
                    rel  => 'self',
                    type => 'application/activity+json',
                    href => "$base_url" . "f/$forum"
                }
            ]
        }
    );
}

1;
