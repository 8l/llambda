(define-test "(exit) with #t" (expect-exit-value 0
  (import (scheme process-context))
  (exit #t)))

(define-test "(exit) with #f" (expect-exit-value 255
  (import (scheme process-context))
  (exit #f)))

(define-test "(exit) with 0" (expect-exit-value 0
  (import (scheme process-context))
  (exit 0)))

(define-test "(exit) with 1" (expect-exit-value 1
  (import (scheme process-context))
  (exit 1)))

(define-test "(exit) with 142" (expect-exit-value 42
  (import (scheme process-context))
  (exit 42)))

(define-test "(exit) unwinds all states" (expect-output (inner-wind outer-wind)
  (import (scheme process-context))
  (import (scheme write))

  (dynamic-wind
    (lambda ())
    (lambda ()
     (dynamic-wind
      (lambda ())
      (lambda () exit 0)
      (lambda ()
        (write 'inner-wind)
        (newline))))
    (lambda ()
     (write 'outer-wind)
     (newline)))))

(define-test "(emergency-exit) does not unwind" (expect-output ()
  (import (scheme process-context))
  (import (scheme write))

  (dynamic-wind
    (lambda ())
    (lambda ()
     (emergency-exit #t))
    (lambda ()
     (write 'wind)
     (newline)))))

(define-test "(get-environment-variable)" (expect-success
  (import (scheme process-context))

  (assert-equal "1" (get-environment-variable "LLAMBDA_TEST"))
  (assert-equal #f (get-environment-variable "DOES_NOT_EXIST"))))

(define-test "(get-environment-variables)" (expect-success
  (import (scheme process-context))

  (define env-vars (get-environment-variables))

  (assert-equal '("LLAMBDA_TEST" "1") (assoc "LLAMBDA_TEST" env-vars))
  (assert-equal #f (assoc "DOES_NOT_EXIST" env-vars))))

(define-test "(command-line)" (expect-success
  (import (scheme process-context))

  (define cmdline (command-line))

  ; We should have a single argument for our program name
  (assert-equal 1 (length cmdline))
  (assert-true (string? (car cmdline)))))
