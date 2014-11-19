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
void exit_process(struct intr_frame *f UNUSED, int error_code);
bool check_valid_ptr (void* vaddr);
bool check_valid_buffer (void* vaddr);
int next_valid_handle();

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
      f->eax = process_execute (args[1]);    
    } else {
      f->eax = -1;
    }  
  } else if (args[0] == SYS_HALT) {
    shutdown_power_off();
  } else if (args[0] == SYS_WAIT) {
    f->eax = process_wait(args[1]);
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
      if (assign_handle == -1) {
        f->eax = -1;
      } else {
        struct file * opened = filesys_open (args[1]);
        if(opened == NULL){
          f->eax = -1;
        }else{
          thread_current()->thread_files.open_files[assign_handle] = opened;
          thread_current()->thread_files.file_valid[assign_handle] = 1;
          f->eax = assign_handle;
        }
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
    if(!check_valid_buffer(args[2]) && check_valid_ptr(&args[3])){
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
      }else if(fd == 0){
        f->eax = input_getc();
      }else{
        f->eax = -1;
      }
    }
  } else if (args[0] == SYS_WRITE) {
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
      }else if(fd == 1){
        putbuf(args[2], args[3]);
      }else{
        f->eax = -1;
      }
    }
  } else if (args[0] == SYS_SEEK){
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
  } else if (args[0] == SYS_TELL){
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
  } else if (args[0] == SYS_CLOSE){
    int fd = args[1];
    if(fd && fd >= 2 && fd < 128 && thread_current()->thread_files.file_valid[fd]){
      thread_current()->thread_files.file_valid[fd] = 0;
    }
  }
}

void exit_process(struct intr_frame *f UNUSED, int error_code){
  thread_current()->exit_code = error_code;
  f->eax = error_code;
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

