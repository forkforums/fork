package Fork::Task::NotifyPeers;

use Moo;

with 'Fork::Task';

has event => (
    is       => 'ro',
    required => 1,
);

has activitypub_service => (
    is      => 'lazy',
    default => sub {
        Fork::Service::ActivityPub->new( dbh => shift->dbh );
    }
);

sub execute {
    my ($self) = @_;

# Here we will notify peers about the event. We will sign the event data and send it to each peer's inbox.
    my $event_data = $self->event;
    my $peers      = $self->activitypub_service->get_peers();
}

1;
