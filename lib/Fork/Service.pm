package Fork::Service;

use Moo::Role;

has dbh => ( is => 'ro', required => 1 );

1;
