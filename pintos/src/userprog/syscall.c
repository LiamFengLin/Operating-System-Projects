#include "userprog/syscall.h"
#include <stdio.h>
#include <syscall-nr.h>
#include "threads/interrupt.h"
#include "threads/thread.h"
#include "threads/vaddr.h"
#include "userprog/pagedir.h"
#include "userprog/process.h"

static void syscall_handler (struct intr_frame *);
static bool check_valid_ptr (void* vaddr);
static bool check_valid_buffer (void* vaddr);

void
syscall_init (void) 
{
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
}

enum intr_level old_level;

  ASSERT (is_thread (t));

  old_level = intr_disable ();
  ASSERT (t->status == THREAD_BLOCKED);
  list_push_back (&ready_list, &t->elem);
  t->status = THREAD_READY;
  intr_set_level (old_level);

static void
syscall_handler (struct intr_frame *f UNUSED) 
{
  uint32_t* args = ((uint32_t*) f->esp);
  if (args[0] == SYS_EXIT) {
    struct lock l = thread_current()->parent_info->child_wait_status->race_lock;
    enum intr_level old_level;
    old_level = intr_disable ();
    sema_up (thread_current()->parent_info->child_wait_status->sema_dead);
    thread_current()->parent_info->child_wait_status->ref_count--;
    thread_current()->parent_info->child_wait_status->exit_status = (int) args[1];
    f->eax = args[1];
    if (thread_current()->parent_info->child_wait_status->ref_count == 0) {
      list_remove (thread_current()->parent_info->elem_in_parent);
      free (thread_current()->parent_info);
    
    struct list_elem f;
    for (e = list_begin (thread_current()->children_info); e != list_end (thread_current()->children_info); e = f) {
      struct process_info *c_info = list_entry (e, struct children_info, elem_in_parent);
      c_info->wait_status->ref_count--;
      f = list_next(e);
      if (c_info->wait_status->ref_count == 0) {
        list_remove (e);
        free (c_info);
      }
    }

    intr_set_level (old_level);
    thread_exit();
  } else if (args[0] == SYS_NULL) {
    f->eax = args[1] + 1;
  } else if (args[0] == SYS_WRITE) {
    printf("%s\n", args[2]);
  } else if (args[0] == SYS_EXEC) {
    if (check_valid_buffer(args[1])) {
      tid_t new_process;
      new_process = process_execute (args[1]);
      if (new_process == TID_ERROR) {
        f->eax = -1;
      } else {
        struct list_elem *e;
        bool found = false;
        for (e = list_begin (thread_current()->children_info); e != list_end (thread_current()->children_info); e = list_next (e)) {
          struct process_info *p_info = list_entry (e, struct process_info, elem_in_parent);
          if (p_info->child_wait_status->child_tid == new_process && p_info->child_wait_status->ref_count != 2) {
            list_remove (e);
            free (p_info);
          }
        }
        for (e = list_begin (thread_current()->children_info); e != list_end (thread_current()->children_info); e = list_next (e)) {
          struct process_info *p_info = list_entry (e, struct process_info, elem_in_parent);
          if (p_info->child_wait_status->child_tid == new_process && p_info->success) {
            f->eax = new_process;
            found = true;
            break;
          }
        }
        if (!found) {
          f->eax = -1;
        }
      }
    } else {
      f->eax = -1;
      thread_exit();
    }
  } else if (args[0] == SYS_HALT) {
    shutdown_power_off();
  } else if (args[0] == SYS_WAIT) {
    struct list_elem *e;
    struct process_info *p_info;
    bool found = false;
    for (e = list_begin (thread_current()->children_info); e != list_end (thread_current()->children_info); e = list_next (e)) {
      p_info = list_entry (e, struct process_info, elem_in_parent);
      if (p_info->child_wait_status->child_tid == new_process) {
        found = true;
        if (p_info->child_wait_status->ref_count == 2) {
          sema_down (p_info->child_wait_status->sema_dead);
          f->eax = p_info->child_wait_status;
          break;
        } else if (p_info->child_wait_status->wait_called) {
          f->eax = -1;
          thread_exit();
          break;
        } 
      } 
    }
    if (!found) {
      f->eax = -1;
      thread_exit();
    }
  }
}

static bool
check_valid_buffer (void* vaddr) {
  while (true) {
    if (check_valid_ptr (vaddr)) {
      uint32_t* pd = thread_current()->pagedir;
      void* new_pd = pagedir_get_page (pd, vaddr);
      if (new_pd == NULL) {
        return false;
      } else {
        if (*((char*) vaddr) == '\0') {
          return true;
        }
        vaddr = ((char*) vaddr) + 1;
      }
    } else {
      return false;
    } 
  } 
}

static bool
check_valid_ptr (void* vaddr)
{
  return vaddr && is_user_vaddr (vaddr);
  //return is_user_vaddr (vaddr);
}