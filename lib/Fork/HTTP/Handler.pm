package Fork::HTTP::Handler;

use Moo::Role;

use Carp 'croak';
use Template;

my $tt = Template->new(
    {
        INCLUDE_PATH => ['templates'],
        ENCODING     => 'UTF-8',
        TRIM         => 1,
    }
) or croak Template->error;

has _json => ( is => 'ro', required => 1 );
has req   => ( is => 'ro', required => 1 );
has res   => ( is => 'ro', required => 1 );
has env   => ( is => 'ro', required => 1 );
has dbh   => ( is => 'ro', required => 1 );

sub status {
    my ( $self, $status ) = @_;
    if ( defined $status ) {
        $self->res->status($status);
    }
    return $self;
}

sub header {
    my ( $self, $field, $value ) = @_;
    if ( defined $field && defined $value ) {
        $self->res->header( $field => $value );
    }
    return $self;
}

sub json {
    my ( $self, $data ) = @_;
    my $json = $self->_json->encode($data);
    $self->header( 'Content-Type' => 'application/json; charset=utf-8' );
    $self->res->body($json);
    return $self;
}

sub template {
    my ( $self, $template_name, $vars ) = @_;
    my $output;
    $tt->process( $template_name . '.tt', $vars, \$output )
      or die "Template processing error: " . $tt->error;
    return $self->html($output);
}

sub redirect {
    my ( $self, $url, $status ) = @_;
    $status ||= 302;
    $self->status($status);
    $self->header( Location => $url );
    $self->res->body('');
    return $self;
}

sub html {
    my ( $self, $html ) = @_;
    $self->header( 'Content-Type' => 'text/html; charset=utf-8' );
    $self->res->body($html);
    return $self;
}

sub error {
    my ( $self, $status, $message ) = @_;
    $self->status($status);
    $self->header( 'Content-Type' => 'text/plain; charset=utf-8' );
    $self->res->body($message);
    return $self;
}

1;
