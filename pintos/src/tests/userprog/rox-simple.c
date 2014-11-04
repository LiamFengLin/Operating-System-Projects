/* Ensure that the executable of a running process cannot be
   modified. */

#include <syscall.h>
#include "tests/lib.h"
#include "tests/main.h"

void
test_main (void) 
{
  int handle;
  char buffer[16];
  
  CHECK ((handle = open ("rox-simple")) > 1, "open \"rox-simple\"");
  // msg("file length: %d", filesize(handle));
  // msg("position: %d", tell(handle));
  CHECK (read (handle, buffer, sizeof buffer) == (int) sizeof buffer,
         "read \"rox-simple\"");
  // msg("buffer size is: %d", (int) sizeof buffer);
  CHECK (write (handle, buffer, sizeof buffer) == 0,
         "try to write \"rox-simple\"");
  // msg("wrote %d bytes", write (handle, buffer, sizeof buffer));
}
