package Fork;

use strict;
use warnings;

use Exporter 'import';

our @EXPORT_OK = qw(app);

use Plack::Builder;

my $app = do {
    require Fork::HTTP;
    Fork::HTTP->new->to_app;
};

sub app {
    builder {
        enable "Plack::Middleware::Static",
          path => qr{^/(?:assets|css|fonts|images|js|styles)/},
          root => "static";
        enable "Plack::Middleware::ContentLength";
        $app;
    };
}

1;
