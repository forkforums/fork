package Fork::Crypt;

use strict;
use warnings;

use Exporter 'import';

use Crypt::OpenSSL::RSA;

our @EXPORT_OK = qw( generate_key_pair );

sub generate_key_pair {
    my $key         = Crypt::OpenSSL::RSA->generate_key(2048);
    my $private_key = $key->get_private_key_string;
    my $public_key  = $key->get_public_key_string;
    return ( $private_key, $public_key );
}

1;
