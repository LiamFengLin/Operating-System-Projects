#ifndef THREADS_SYNCH_H
#define THREADS_SYNCH_H

#include <list.h>
#include <stdbool.h>

#define max(a,b) \
 ({ __typeof__ (a) _a = (a); \
     __typeof__ (b) _b = (b); \
   _a > _b ? _a : _b; })

/* A counting semaphore. */
struct semaphore 
  {
    unsigned value;             /* Current value. */
    struct list waiters;        /* List of waiting threads. */
  };

void sema_init (struct semaphore *, unsigned value);
void sema_down (struct semaphore *);
bool sema_try_down (struct semaphore *);
void sema_up (struct semaphore *);
void sema_self_test (void);

/* Lock. */
struct lock
  {
    struct thread *holder;        /* Thread holding lock (for debugging). */
    struct semaphore semaphore;   /* Binary semaphore controlling access. */
    int largest_donated_priority; /* Store the largest donated priority in semaphore's waiters */
    struct list_elem holder_elem;
  };

void lock_init (struct lock *);
void lock_acquire (struct lock *);
bool lock_try_acquire (struct lock *);
void lock_release (struct lock *);
bool lock_held_by_current_thread (const struct lock *);

bool scheduler_less (const struct list_elem *a, const struct list_elem *b, void *aux);
bool scheduler_less_allelem (const struct list_elem *a, const struct list_elem *b, void *aux);
bool scheduler_less_sema_elem (const struct list_elem *a, const struct list_elem *b, void *aux);
bool held_lock_less (const struct list_elem *a, const struct list_elem *b, void *aux);
int get_donated_priority (struct thread *t);

/* Condition variable. */
struct condition 
  {
    struct list waiters;        /* List of waiting threads. */
  };

void cond_init (struct condition *);
void cond_wait (struct condition *, struct lock *);
void cond_signal (struct condition *, struct lock *);
void cond_broadcast (struct condition *, struct lock *);

bool cond_less (const struct list_elem *a, const struct list_elem *b, void *aux);

/* Optimization barrier.

   The compiler will not reorder operations across an
   optimization barrier.  See "Optimization Barriers" in the
   reference guide for more information.*/
#define barrier() asm volatile ("" : : : "memory")

#endif /* threads/synch.h */
