#ifndef USERPROG_PROCESS_H
#define USERPROG_PROCESS_H

#include "threads/thread.h"
#include "threads/synch.h"

tid_t process_execute (const char *file_name);
int process_wait (tid_t);
void process_exit (void);
void process_activate (void);

struct wait_status {
  struct lock race_lock;
  struct semaphore sema_dead;
  tid_t child_tid;                      /* used by parent to identify the child */
  int ref_count;                        /* a reference count to identify whether the parent or the child or both are dead */
  int exit_status;                      /* used to store the child’s exit_status, if needed */
  bool wait_called;                     /* if parent have already called wait on this child */
};

struct process_info {
  struct list_elem elem_in_parent;      /* Keep this struct inside parent's child wait status list*/
  struct wait_status child_wait_status; /* Stores infomation */
  bool success;                         /* If children has successfully loaded*/
  struct semaphore sema_load;           /* Ensure parent read the loading success information after the child has finished loading */
  char *fn_copy;                        /* Cpoy of file name including argument passing*/
};

void process_info_init(struct process_info *info, tid_t child_tid);
void wait_status_init(struct wait_status *status, tid_t child_tid);
char* get_arg(int arg_num, char* args);
int num_of_args(char* args);
bool check_buffer_overflow(char* args);

#endif /* userprog/process.h */
