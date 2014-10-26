#ifndef USERPROG_PROCESS_H
#define USERPROG_PROCESS_H

#include "threads/thread.h"

tid_t process_execute (const char *file_name);
int process_wait (tid_t);
void process_exit (void);
void process_activate (void);

struct wait_status {
  struct lock race_lock;
  struct semaphore sema_dead;
  tid_t child_tid;  // used by parent to identify the child
  int ref_count;  // a reference count to identify whether the parent or the child or both are dead
  int exit_status;  // used to store the child’s exit_status, if needed
};

struct process_info {
  struct list_elem elem_in_parent;
  struct wait_status child_wait_status;
  bool success;
  struct semaphore sema_load;
  char *fn_copy;
};

#endif /* userprog/process.h */
