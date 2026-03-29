package Fork::HTTP::Handler::API::Actor;

use Moo;

use Fork::Service::ActivityPub;

with 'Fork::HTTP::Handler';

has activitypub_service => (
    is      => 'lazy',
    default => sub {
        my $self = shift;
        Fork::Service::ActivityPub->new( req => $self->req, dbh => $self->dbh );
    }
);

sub get_actor {
    my ( $self, $args ) = @_;

    my $actor_id = $args->{actor_id};

    if ( !$actor_id ) {
        $self->error( 400, 'Bad Request' );
    }

    my $actor = $self->activitypub_service->get_actor($actor_id);

    if ( !$actor ) {
        $self->error( 404, 'Not Found' );
    }

    return $self->json($actor);
}

1;
