#include "userprog/syscall.h"
#include <stdio.h>
#include <syscall-nr.h>
#include "threads/interrupt.h"
#include "threads/thread.h"
#include "threads/vaddr.h"
#include "threads/malloc.h"
#include "userprog/pagedir.h"
#include "userprog/process.h"
#include "devices/shutdown.h"
#include "filesys/filesys.h"
#include "filesys/file.h"

static void syscall_handler (struct intr_frame *);
bool check_valid_ptr (void* vaddr);
bool check_valid_buffer (void* vaddr);
void exit_process(struct intr_frame *f UNUSED, int error_code);

void
syscall_init (void) 
{
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
}

static void
syscall_handler (struct intr_frame *f UNUSED) 
{

  /* Check the input. The pointer should be valid. The input buffer should not excedd user memory space. 
     If this check fails, exit the program immediately with exit code -1 */
  if (!check_valid_ptr(f->esp)) {
    exit_process(f, -1);
  } else {
    uint32_t* pd = thread_current()->pagedir;
    void* new_pd = pagedir_get_page (pd, f->esp);
    if (new_pd == NULL) {
      exit_process(f, -1);
    }
  } 
  uint32_t* args = ((uint32_t*) f->esp);
  if(!check_valid_ptr((void *) &args[1])){
    exit_process(f, -1);
  }

  /* Identify the type of syscall*/
  if (args[0] == SYS_EXIT) {                /* SYSCALL: void exit (int status) */
  exit_process(f, (int) args[1]); 

  } else if (args[0] == SYS_NULL) {         /* SYSCALL: int null (int i) */
  f->eax = args[1] + 1;

  } else if (args[0] == SYS_EXEC) {         /* SYSCALL: pid t exec (const char *cmd line) */

    if (check_valid_buffer(args[1])) {      /* Ensure that the input buffer is valid */

      enum intr_level old_level;            /* Disable interrupt to eliminate race condition because will perform list operation*/
      old_level = intr_disable ();

      tid_t new_process;
      new_process = process_execute (args[1]);
      if (new_process == TID_ERROR) {
        f->eax = -1;
      } else {                              /* Find the wait_status struct and check success. If not success, set exit code -1*/
        struct list_elem *e;
        struct list_elem *g;
        bool found = false;
        struct process_info *p_info;
        for (e = list_begin (&thread_current()->children_info); e != list_end (&thread_current()->children_info); e = list_next (e)) {
          p_info = list_entry (e, struct process_info, elem_in_parent);
          if (p_info->child_wait_status.child_tid == new_process && p_info->success) {
            found = true;
            f->eax = new_process;
            break;
          }
        }
        if (!found) {
          f->eax = -1;
        }
      }
        intr_set_level (old_level);         /* Re-enable interrupt*/
    } else {
      f->eax = -1;
    }
  } else if (args[0] == SYS_HALT) {       /* SYSCALL: void halt (void) */
    shutdown_power_off();
  } else if (args[0] == SYS_WAIT) {       /* SYSCALL:  int wait (pid t pid) */

    struct list_elem *e;
    struct process_info *p_info;

    enum intr_level old_level;
    old_level = intr_disable ();

    bool found = false;
    for (e = list_begin (&thread_current()->children_info); e != list_end (&thread_current()->children_info); e = list_next (e)) {
      p_info = list_entry (e, struct process_info, elem_in_parent);
      if (p_info->child_wait_status.child_tid == args[1]) {
        found = true;
        if (p_info->child_wait_status.wait_called) {                    /* Called wait twice*/
        f->eax = -1;
        } else if (p_info->child_wait_status.ref_count == 2) {          /* Child and parent running*/
        p_info->child_wait_status.wait_called = true;
        intr_set_level (old_level);
        sema_down (&(p_info->child_wait_status.sema_dead));
        old_level = intr_disable ();
        f->eax = p_info->child_wait_status.exit_status;
        } else if (p_info->child_wait_status.ref_count == 1) {          /* Child already exited.*/
        p_info->child_wait_status.wait_called = true;
        f->eax = p_info->child_wait_status.exit_status;
      }
      break;
    } 
  }

    intr_set_level (old_level);

    if (!found) {
      f->eax = -1;
    }

  } else if (args[0] == SYS_CREATE){                    /* SYSCALL: bool create (const char *file, unsigned initial size) */
    if(!args[1] || args[1] == "" || !check_valid_buffer(args[1])){
          exit_process(f, -1);                              /* Malicious buffer, exit the program immediately with exit code -1*/
    }else{
      f->eax = filesys_create (args[1], args[2]);
    }
  } else if (args[0] == SYS_REMOVE){
    if(!args[1] || args[1] == "" || !check_valid_buffer(args[1])){
      exit_process(f, -1);

    }else{
      f->eax = filesys_remove (args[1]);
    }

  } else if (args[0] == SYS_OPEN){                       /* SYSCALL:  int open (const char *file)*/
    if(args[1] == ""){                                   /* File name cannot be empty*/
      f->eax = -1;
    }else if(!args[1] || !check_valid_buffer(args[1])){
      exit_process(f, -1);
    }else{
      int assign_handle = next_valid_handle();
      struct file * opened = filesys_open (args[1]);
      if(opened == NULL){
        f->eax = -1;
      }else{
        thread_current()->thread_files.open_files[assign_handle] = opened;
        thread_current()->thread_files.file_valid[assign_handle] = 1;
        f->eax = assign_handle;
      }
    }
  } else if (args[0] == SYS_FILESIZE){                                /* SYSCALL: int filesize (int fd) */
    int fd = args[1];
    if(args[1] && fd >= 2 && fd < 128 && thread_current()->thread_files.file_valid[fd]){
      struct file* opened = thread_current()->thread_files.open_files[fd];
      f->eax = file_length(opened);
    }else{
      f->eax = -1;
    }
  } else if (args[0] == SYS_READ){                                    /* SYSCALL: int read (int fd, const void *buffer, unsigned size)*/
    if(!check_valid_buffer(args[2]) && check_valid_ptr(&args[3])){    /* Malicious buffer, exit the program immediately with exit code -1*/
    exit_process(f, -1);
    }else{
      int fd = args[1];
      if(fd && fd >= 2 && fd < 128 && thread_current()->thread_files.file_valid[fd]){
        struct file* opened = thread_current()->thread_files.open_files[fd];
        if(opened){
          f->eax = file_read(opened, args[2], args[3]);
        }else{
          f->eax = -1;
        }
      }else if(fd == 0){                                                 /* stdin*/
        f->eax = input_getc();
      }else{
        f->eax = -1;
      }
    }

  } else if (args[0] == SYS_WRITE) {                                /* SYSCALL: int write (int fd, const void *buffer, unsigned size)*/
    if(!check_valid_buffer(args[2]) && check_valid_ptr(&args[3])){
      exit_process(f, -1);
    }else{
      int fd = args[1];
      if(fd && fd >= 2 && fd < 128 && thread_current()->thread_files.file_valid[fd]){
        struct file* opened = thread_current()->thread_files.open_files[fd];
        if(opened && check_valid_ptr(&args[1]) && check_valid_buffer(args[2])){
          f->eax = file_write(opened, args[2], args[3]);
        }else{
          f->eax = -1;
        }
      }else if(fd == 1){                                                 /* std out*/
        putbuf(args[2], args[3]);
      }else{
        f->eax = -1;
      }
    }

  } else if (args[0] == SYS_SEEK){                                    /* SYSCALL: void seek (int fd, unsigned position) */
    int fd = args[1];
    if(fd && fd >= 2 && fd < 128 && thread_current()->thread_files.file_valid[fd]){
      struct file* opened = thread_current()->thread_files.open_files[fd];
      if(opened){
        file_seek(opened, args[2]);
      }else{
        f->eax = -1;
      }
    }else{
      f->eax = -1;
    }
      } else if (args[0] == SYS_TELL){                                     /* SYSCALL:  unsigned tell (int fd) */
    int fd = args[1];
    if(fd && fd >= 2 && fd < 128 && thread_current()->thread_files.file_valid[fd]){
      struct file* opened = thread_current()->thread_files.open_files[fd];
      if(opened){
        f->eax = file_tell(opened);      
      }else{
        f->eax = -1;
      }
    }else{
      f->eax = -1;
    }
      } else if (args[0] == SYS_CLOSE){                                    /* SYSCALL: void close (int fd)*/
    int fd = args[1];
    if(fd && fd >= 2 && fd < 128 && thread_current()->thread_files.file_valid[fd]){
      thread_current()->thread_files.file_valid[fd] = 0;
    }
  }
}

/* Store the exit code and exit process by calling thread_exit();*/
void exit_process(struct intr_frame *f UNUSED, int error_code){
  thread_current()->exit_code = error_code;
  f->eax = error_code;
  thread_exit();

}


/* Check the buffer at *vaddr is within user program. */
bool
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

/* Check that vaddr is not NULL and is user address. */
bool
check_valid_ptr (void* vaddr)
{
  return vaddr && is_user_vaddr (vaddr);
}

/* Return current thread's smallest ununsed int file descriptor. Return -1 if all fd are used. */
int next_valid_handle(){
  int* valid_bits =  thread_current()->thread_files.file_valid;
  int i;
  for(i = 2; i < 128; i ++){
    if(!valid_bits[i]){
      return i;
    }
  }
  return -1;
}