package Fork::HTTP::Handler::API::Inbox;

use Moo;

sub ingest {
    my ( $self, $args ) = @_;

    my $data = $self->req->body_parameters->get('data');

    if ( !$data ) {
        return $self->error( 400, 'Missing data parameter' );
    }
}

1;
