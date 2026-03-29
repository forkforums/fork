package Fork::Service::Peer;

use Moo;

with 'Fork::Service';

sub add_peer {
    my ( $self, $peer_url ) = @_;
    my $sth = $self->dbh->prepare('INSERT INTO peers ( url ) VALUES ( ? )');
    $sth->execute($peer_url);
    return $peer_url;
}

sub add_peer_forum {
    my ( $self, $peer_url, $forum_id ) = @_;
    my $sth = $self->dbh->prepare(
        'INSERT INTO peer_forums ( peer_url, forum_id ) VALUES ( ?, ? )');
    $sth->execute( $peer_url, $forum_id );
    return { peer_url => $peer_url, forum_id => $forum_id };
}

1;
