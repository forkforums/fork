package Fork::Service::ActivityPub;

use Moo;

with 'Fork::Service';

has req => ( is => 'ro', required => 1 );

sub get_actor {
    my ( $self, $actor_id ) = @_;
    my $sth =
      $self->dbh->prepare('SELECT id, public_key FROM forums WHERE id = ?');
    $sth->execute($actor_id);
    my $forum = $sth->fetchrow_hashref;

    return unless $forum;

    my $base = $self->req->base;

    $base .= '/' unless $base =~ m{/$};

    my $outbox = $base . $forum->{id} . "/outbox";
    my $inbox  = $base . $forum->{id} . "/inbox";

    return {
        id                => $forum->{id},
        type              => 'Group',
        preferredUsername => $forum->{id},
        inbox             => $inbox,
        outbox            => $outbox,
        publicKey         => $forum->{public_key}
    };
}

sub forward_to_peers {
    my ( $self, $activity ) = @_;
    my $sth = $self->dbh->prepare(
        'SELECT peer_url FROM peer_forums WHERE forum_id = ?');
    $sth->execute( $activity->{target} );
    my @peer_urls;
    while ( my $row = $sth->fetchrow_hashref ) {
        push @peer_urls, $row->{peer_url};
    }

    for my $peer_url (@peer_urls) {
        eval {
            HTTP::Tiny->new->post(
                "$peer_url/inbox",
                {
                    headers =>
                      { 'Content-Type' => 'application/activity+json' },
                    content => JSON::encode_json($activity)
                }
            );
        };
        warn "Failed to forward activity to $peer_url: $@" if $@;
    }
}

1;
