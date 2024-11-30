#!/usr/bin/perl

use Mojolicious::Lite -signatures;
use Mojo::JSON qw(decode_json encode_json);

my $i = 0;

get '/json' => sub ($c) {
  $i++;
  $c->render(json => {foo => $i, bar => 'hello!', baz => 127.2});
};

app->start('daemon', '-l', 'http://127.0.0.1:8080');
