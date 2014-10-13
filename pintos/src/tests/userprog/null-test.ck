# -*- perl -*-
use strict;
use warnings;
use tests::tests;
check_expected (IGNORE_USER_FAULTS => 1, [<<'EOF']);
(null-test) begin
(null-test) end
null-test: exit(0)
EOF
pass;
