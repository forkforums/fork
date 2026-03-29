package Fork::Task;

use Moo::Role;
use Try::Tiny;
use POSIX ();

has dbh => (
    is       => 'ro',
    required => 1,
);

sub run {
    my ($self) = @_;

    my $pid = fork();

    if ( !$pid ) {
        try {
            $self->execute();
        }
        catch {
            warn "Error executing task: $_";
        };
        POSIX::_exit(0);
    }
}

1;
