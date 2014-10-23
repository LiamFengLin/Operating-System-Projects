#include "userprog/syscall.h"
#include <stdio.h>
#include <syscall-nr.h>
#include "threads/interrupt.h"
#include "threads/thread.h"

static void syscall_handler (struct intr_frame *);

void
syscall_init (void) 
{
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
}

static void
syscall_handler (struct intr_frame *f UNUSED) 
{
  uint32_t* args = ((uint32_t*) f->esp);
  if (args[0] == SYS_EXIT) {
    f->eax = args[1];
    thread_exit();
  } else if (args[0] == SYS_NULL) {
  	f->eax = args[1] + 1;
  } else if (args[0] == SYS_WRITE) {
  	printf("%s\n", args[2]);
  }
}
