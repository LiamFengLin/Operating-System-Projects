/* Tests the exit system call. */

#include "tests/lib.h"
#include "tests/main.h"
#include <stdio.h>

void
test_main (void) 
{
  printf("exit");
  exit (57);
  fail ("should have called exit(57)");
}
