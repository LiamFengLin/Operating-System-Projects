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
  if (args[0] == SYS_EXIT) {
    exit_process(f, (int) args[1]);  
  } else if (args[0] == SYS_NULL) {
    f->eax = args[1] + 1;
  } else if (args[0] == SYS_EXEC) {
    if (check_valid_buffer(args[1])) {

      enum intr_level old_level;
      old_level = intr_disable ();

      tid_t new_process;
      new_process = process_execute (args[1]);
      if (new_process == TID_ERROR) {
        f->eax = -1;
      } else {
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
      intr_set_level (old_level);
    } else {
      f->eax = -1;
    }
  } else if (args[0] == SYS_HALT) {
    shutdown_power_off();
  } else if (args[0] == SYS_WAIT) {
    struct list_elem *e;
    struct process_info *p_info;

    enum intr_level old_level;
    old_level = intr_disable ();

    bool found = false;
    for (e = list_begin (&thread_current()->children_info); e != list_end (&thread_current()->children_info); e = list_next (e)) {
      p_info = list_entry (e, struct process_info, elem_in_parent);
      if (p_info->child_wait_status.child_tid == args[1]) {
        found = true;
        if (p_info->child_wait_status.wait_called) {
          f->eax = -1;
        } else if (p_info->child_wait_status.ref_count == 2) {
          p_info->child_wait_status.wait_called = true;
          intr_set_level (old_level);
          sema_down (&(p_info->child_wait_status.sema_dead));
          f->eax = p_info->child_wait_status.exit_status;
        } else if (p_info->child_wait_status.ref_count == 1) {
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
  } else if (args[0] == SYS_CREATE){
    if(!args[1] || args[1] == "" || !check_valid_buffer(args[1])){
      exit_process(f, -1);
    }else{
      f->eax = filesys_create (args[1], args[2]);
    }
  } else if (args[0] == SYS_REMOVE){
    if(!args[1] || args[1] == "" || !check_valid_buffer(args[1])){
      exit_process(f, -1);

    }else{
      f->eax = filesys_remove (args[1]);
    }
  } else if (args[0] == SYS_OPEN){
    if(args[1] == ""){
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
  } else if (args[0] == SYS_FILESIZE){
    int fd = args[1];
    if(args[1] && fd >= 2 && fd < 128 && thread_current()->thread_files.file_valid[fd]){
      struct file* opened = thread_current()->thread_files.open_files[fd];
      f->eax = file_length(opened);
    }else{
      f->eax = -1;
    }
  } else if (args[0] == SYS_READ){
    if(!check_valid_buffer(args[2]) && check_valid_ptr(&args[1])){
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
      }else{
        f->eax = -1;
      }
    }
  } else if (args[0] == SYS_WRITE) {
    int fd = args[1];
    if(fd && fd >= 2 && fd < 128 && thread_current()->thread_files.file_valid[fd]){
      struct file* opened = thread_current()->thread_files.open_files[fd];
      if(opened && check_valid_ptr(&args[1]) && check_valid_buffer(args[2])){
        f->eax = file_write(opened, args[2], args[3]);
      }else{
        f->eax = -1;
      }
    }else if(fd == 1){
      printf("%s", args[2]);
    }else{
      f->eax = -1;
    }
    
  } else if (args[0] == SYS_SEEK){

  } else if (args[0] == SYS_TELL){

  } else if (args[0] == SYS_CLOSE){

  }
}

void exit_process(struct intr_frame *f UNUSED, int error_code){
  if (thread_current()->parent_info) {
    sema_up (&(thread_current()->parent_info->child_wait_status.sema_dead));
    thread_current()->parent_info->child_wait_status.ref_count--;
    thread_current()->parent_info->child_wait_status.exit_status = error_code;
    if (thread_current()->parent_info->child_wait_status.ref_count == 0) {
      list_remove (&thread_current()->parent_info->elem_in_parent);
      free (thread_current()->parent_info);
    }
  }
  struct list_elem *g;
  struct list_elem *e;
  struct process_info *c_info;
  for (e = list_begin (&thread_current()->children_info); e != list_end (&thread_current()->children_info); e = g) {
    c_info = list_entry (e, struct process_info, elem_in_parent);
    c_info->child_wait_status.ref_count--;
    g = list_next(e);
    if (c_info->child_wait_status.ref_count == 0) {
      list_remove (e);
      free (c_info);
    }
  }
  f->eax = error_code;
  printf("%s: exit(%d)\n", thread_current()->name, f->eax);
  thread_exit();

}

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

bool
check_valid_ptr (void* vaddr)
{
  return vaddr && is_user_vaddr (vaddr);
}

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