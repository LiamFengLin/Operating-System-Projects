#include <random.h>
#include "tests/lib.h"
#include "tests/main.h"

#define checkpoint1

int
main (int argc UNUSED, char *argv[]) 
{
  #ifndef checkpoint1
    test_name = argv[0];
  #endif

  #ifdef checkpoint1
    (void) argv;
    random_init(0);
    test_main();
    return 0;
  #else
    msg ("begin");
    random_init (0);
    test_main ();
    msg ("end");
    return 0;
  #endif
}
