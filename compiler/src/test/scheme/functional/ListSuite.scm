(define-test "static (pair?)" (expect-static-success
  (assert-true  (pair? '(a . b)))
  (assert-true  (pair? '(a  b c)))
  (assert-false (pair? '()))
  (assert-false (pair? #(a b)))))

(define-test "dynamic (pair?)" (expect-success
  (assert-true  (pair? (typeless-cell '(a . b))))
  (assert-true  (pair? (typeless-cell '(a  b c))))
  (assert-false (pair? (typeless-cell '())))
  (assert-false (pair? (typeless-cell '())))))

(define-test "static (null?)" (expect-static-success
  (assert-true  (null? '()))
  (assert-false (null? '(a b c)))
  (assert-false (null? #(a b c)))))

(define-test "dynamic (null?)" (expect-success
  (assert-true  (null? (typeless-cell '())))
  (assert-false (null? (typeless-cell '(a b c))))
  (assert-false (null? (typeless-cell #(a b c))))))

(define-test "static (list?)" (expect-static-success
   (assert-true  (list? '(a b c)))
   (assert-true  (list? '()))
   (assert-false (list? '(a . b)))))

(define-test "(cons)" (expect-static-success
  (assert-equal '(a)
                (cons 'a '()))

  (assert-equal '((a) b c d)
                (cons '(a) '(b c d)))

  (assert-equal '("a" b c)
                (cons "a" '(b c)))

  (assert-equal '(a . 3)
                (cons 'a 3))

  (assert-equal '((a b) . c)
                (cons '(a b) 'c))

  (assert-equal 5
    (let ((x (cons 2 3)))
        (+ (car x) (cdr x))))))

(define-test "(car)" (expect-static-success
  (assert-equal 'a (car '(a b c)))
  (assert-equal '(a) (car '((a) b c)))
  (assert-equal 1 (car '(1 . 2)))))

(define-test "(cdr)" (expect-static-success
  (assert-equal '(b c) (cdr '(a b c)))
  (assert-equal '(b c) (cdr '((a) b c)))
  (assert-equal 2 (cdr '(1 . 2)))))

(define-test "(length)" (expect-static-success
  (assert-equal 3 (length '(a b c)))
  (assert-equal 3 (length '(a (b) (c d e))))
  (assert-equal 0 (length '()))))

(define-test "length of improper list fails" (expect-error type-error?
  (length '(1 . 2))))

(define-test "make-list" (expect-success
  (assert-equal '() (make-list 0))
  (assert-equal '() (make-list 0 4.0))
  (assert-equal '(#!unit #!unit #!unit #!unit) (make-list 4))
  (assert-equal '(4.0 4.0 4.0 4.0) (make-list 4 4.0))
  (assert-equal '(#() #()) (make-list 2 (make-vector 0)))))

(define-test "(list-copy) of degenerate lists" (expect-success
  (assert-equal '() (list-copy '()))
  (assert-equal '(1 2 . 3) (list-copy '(1 2 . 3)))
  ; This is allowed by R7RS. Single objects can be considered degenerate forms of improper lists so this makes sense.
  (assert-equal 'a (list-copy 'a))))

(cond-expand
  (immutable-pairs
    (define-test "(list-copy) of non-empty proper list" (expect-success
      (define immutable-list '(1.0 2.0 3.0))
      (define copied-list (list-copy immutable-list))

      (assert-equal '(1.0 2.0 3.0) copied-list))))
  (else
    (define-test "(list-copy) of non-empty proper list" (expect-success
      (define immutable-list '(1.0 2.0 3.0))
      (define copied-list (list-copy immutable-list))
      ; This shouldn't effect the immutable list
      (set-car! copied-list -1.0)

      (assert-equal '(1.0 2.0 3.0) immutable-list)
      (assert-equal '(-1.0 2.0 3.0) copied-list)))))

(define-test "(list)" (expect-static-success
  (assert-true (null? (list)))
  (assert-equal '() (list))
  (assert-equal '(1 2 3) (list 1 2 3))))

(cond-expand
  ((not immutable-pairs)
    (define-test "mutating (list) to improper" (expect-success
      (define test-list (list 1 2 3))
      (set-cdr! test-list 2)

      ; No longer an improper list
      (assert-false (list? test-list))
      (assert-equal '(1 . 2) test-list)))))

(define-test "(append)" (expect-static-success
  (assert-equal '() (append))
  (assert-equal 'a (append 'a))
  (assert-equal '(1 2 3 4 5 6) (append '(1 2) '(3 4) '(5 6)))
  (assert-equal '() (append '() '() '()))
  (assert-equal 'a (append '() 'a))))

(define-test "(append) with non-terminal non-list fails" (expect-error type-error?
  (append '(1 2) 3 '(4 5))))

(define-test "(memq)" (expect-static-success
  (assert-equal '(a b c) (memq 'a '(a b c)))
  (assert-equal '(b c) (memq 'b '(a b c)))
  (assert-false (memq 'a '(b c d)))

  (cond-expand ((not immutable-pairs)
    ; memq isn't recurive
    (assert-false (memq (list 'a) '(b (a) c)))))))

(define-test "(member) is recursive" (expect-static-success
  (assert-equal '((a) c) (member (list 'a) '(b (a) c)))))

; This is technically unspecified for memq because integer comparison is
; unspecified for eq?
(define-test "(memv) on number list" (expect (101 102)
  (memv 101 '(100 101 102))))

(cond-expand ((not immutable-pairs)
  (define-test "(set-car!) of cons" (expect (new-car . old-cdr)
    (define test-cons (cons 'old-car 'old-cdr))
    (set-car! test-cons 'new-car)
    test-cons))

  (define-test "(set-car!) on literal fails" (expect-error mutate-literal-error?
    (set-car! '(old-car . old-cdr) 'new-car)))

  (define-test "(set-cdr!) of cons" (expect (old-car . new-cdr)
    (define test-cons (cons 'old-car 'old-cdr))
    (set-cdr! test-cons 'new-cdr)
    test-cons))

  (define-test "(set-cdr!) on literal fails" (expect-error mutate-literal-error?
    (set-cdr! '(old-car . old-cdr) 'new-cdr)))))

(define-test "(reverse)" (expect-success
  (assert-equal '() (reverse '()))
  (assert-equal '(c b a) (reverse '(a b c)))
  (assert-equal '((e (f)) d (b c) a) (reverse '(a (b c) d (e (f)))))))

(define-test "base cadr procedures" (expect-static-success
  (define test-list '((1 . 2) (3 . 4)))

  (assert-equal 1 (caar test-list))
  (assert-equal '(3 . 4) (cadr test-list))
  (assert-equal 2 (cdar test-list))
  (assert-equal '() (cddr test-list))))

(define-test "association lists" (expect-success
  (define e '((a 1)(b 2)(c 3)))

  (assert-equal '(a 1) (assq 'a e))
  (assert-equal '(b 2) (assq 'b e))
  (assert-equal #f (assq 'd e))

  (cond-expand ((not immutable-pairs)
    (assert-equal #f (assq (list 'a) '(((a)) ((b)) ((c)))))))

  (assert-equal '((a)) (assoc (list 'a) '(((a)) ((b)) ((c)))))

  (assert-equal '(5 7) (assv 5 '((2 3) (5 7) (11 13))))))

(define-test "(list-tail)" (expect-static-success
  (assert-equal '(1 2 3) (list-tail '(1 2 3) 0))
  (assert-equal '(2 3) (list-tail '(1 2 3) 1))
  (assert-equal '(3) (list-tail '(1 2 3) 2))
  (assert-equal '() (list-tail '(1 2 3) 3))))

(define-test "(list-tail) past end of list fails" (expect-error range-error?
  (list-tail '(1 2 3) 4)))

(define-test "(list-tail) on non-list fails" (expect-error type-error?
  ; This is so we don't try to be too clever and directly return the argument when the index is 0
  (list-tail #f 0)))

(define-test "static (list-ref)" (expect-static-success
  (assert-equal 'c (list-ref '(a b c d) 2))
  (assert-equal 'd (list-ref (list 'a 'b 'c 'd) 3))))

(define-test "dynamic (list-ref)" (expect-success
  (assert-equal 'c (list-ref '(a b c d) (typed-dynamic 2 <exact-integer>)))))

(define-test "(list-ref) at exact end of list fails" (expect-error range-error?
  (list-ref '(1 2 3) 3)))

(define-test "(list-ref) past end of list fails" (expect-error range-error?
  (list-ref '(1 2 3) 4)))

(define-test "(filter)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (cond-expand (immutable-pairs
    (ann (filter even? '(3 1 4 1 5 9)) (Listof <exact-integer>))))

  (assert-equal '(0 7 8 8 43 -4) (filter number? '(0 7 8 8 43 -4)))
  (assert-equal '(0 8 8 -4) (filter even? '(0 7 8 8 43 -4)))
  (assert-equal '() (filter symbol? '(0 7 8 8 43 -4)))
  (assert-equal '() (filter even? '()))))

(define-test "(remove)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (cond-expand (immutable-pairs
    (ann (remove even? '(0 7 8 8 43 -4)) (Listof <exact-integer>))))

  (assert-equal '(0 7 8 8 43 -4) (remove symbol? '(0 7 8 8 43 -4)))
  (assert-equal '(7 43) (remove even? '(0 7 8 8 43 -4)))
  (assert-equal '() (remove number? '(0 7 8 8 43 -4)))
  (assert-equal '() (remove even? '()))))

(define-test "(find)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (cond-expand (immutable-pairs
    (ann (find even? '(3 1 4 1 5 9)) (U <exact-integer> #f))))

  (assert-equal 4 (find even? '(3 1 4 1 5 9)))
  (assert-equal #f (find even? '()))
  (assert-equal #f (find symbol? '(3 1 4 1 5 9)))))

(define-test "(find-tail)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (cond-expand (immutable-pairs
    (ann (find-tail even? '(3 1 37 -8 -5 0 0)) (U (Listof <exact-integer>) #f))))

  (assert-equal '(-8 -5 0 0) (find-tail even? '(3 1 37 -8 -5 0 0)))
  (assert-equal #f (find-tail even? '(3 1 37 -5)))))

(define-test "(take-while)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (cond-expand (immutable-pairs
    (ann (take-while number? '(2 18 3 10 22 9)) (Listof <exact-integer>))))

  (assert-equal '(2 18 3 10 22 9) (take-while number? '(2 18 3 10 22 9)))
  (assert-equal '(2 18) (take-while even? '(2 18 3 10 22 9)))
  (assert-equal '() (take-while symbol? '(2 18 3 10 22 9)))
  (assert-equal '() (take-while even? '()))))

(define-test "(drop-while)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (cond-expand (immutable-pairs
    (ann (drop-while even? '(2 18 3 10 22 9)) (Listof <exact-integer>))))

  (assert-equal '() (drop-while number? '(2 18 3 10 22 9)))
  (assert-equal '(3 10 22 9) (drop-while even? '(2 18 3 10 22 9)))
  (assert-equal '(2 18 3 10 22 9) (drop-while symbol? '(2 18 3 10 22 9)))
  (assert-equal '() (drop-while even? '()))))

(define-test "(cons*)" (expect-success
  (import (llambda list))

  (assert-equal '(1 2 3 . 4) (cons* 1 2 3 4))
  (assert-equal '((1 2) 3 4) (cons* '(1 2) '(3 4)))
  (assert-equal '4 (cons* 4))

  (assert-equal '(1 2 3 4 5) (cons* (append '(1 2 3) (typeless-cell '(4 5)))))))

(define-test "(partition)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (let-values (((true-list false-list) (partition (lambda (x) x) '(one 2 3 four five 6))))
              (cond-expand
                (immutable-pairs
                  (ann true-list (Listof (U <symbol> <exact-integer>)))
                  (ann false-list (Listof (U <symbol> <exact-integer>)))))

              (assert-equal true-list '(one 2 3 four five 6))
              (assert-equal false-list '()))

  (let-values (((true-list false-list) (partition symbol? '(one 2 3 four five 6))))
              (cond-expand
                (immutable-pairs
                  (ann true-list (Listof (U <symbol> <exact-integer>)))
                  (ann false-list (Listof (U <symbol> <exact-integer>)))))

              (assert-equal true-list '(one four five))
              (assert-equal false-list '(2 3 6)))

  (let-values (((true-list false-list) (partition number? '(one 2 3 four five 6))))
              (cond-expand
                (immutable-pairs
                  (ann true-list (Listof (U <symbol> <exact-integer>)))
                  (ann false-list (Listof (U <symbol> <exact-integer>)))))

              (assert-equal true-list '(2 3 6))
              (assert-equal false-list '(one four five)))

  (let-values (((true-list false-list) (partition port? '(one 2 3 four five 6))))
              (cond-expand
                (immutable-pairs
                  (ann true-list (Listof (U <symbol> <exact-integer>)))
                  (ann false-list (Listof (U <symbol> <exact-integer>)))))

              (assert-equal true-list '())
              (assert-equal false-list '(one 2 3 four five 6)))

  (cond-expand
    ((not immutable-pairs)
     (let ((input-list (list 1 2 3 4 5)))
       (define (evil-predicate val)
         ; Mutate the list while we're partitioning - this is undefined but shouldn't crash
         (set-cdr! input-list 5)
         #t)


       (guard (condition
                (else 'ignore))
              (partition evil-predicate input-list)))))))

(define-test "(partition) with improper list fails" (expect-error type-error?
  (import (llambda list))
  (partition symbol? '(one 2 3 four five . 6))))

(define-test "(zip)" (expect-success
  (import (llambda list))

  (assert-equal '((one 1 odd) (two 2 even) (three 3 odd)) (zip '(one two three)
                                                               '(1 2 3)
                                                               '(odd even odd even odd even odd even)))

  (assert-equal '((1) (2) (3)) (zip '(1 2 3)))))

(define-test "(xcons)" (expect-static-success
  (import (llambda list))
  (import (llambda typed))

  (cond-expand (immutable-pairs
    (ann (xcons 1 'foo) (Pairof <symbol> <exact-integer>))))

  (assert-equal '(a b c) (xcons '(b c) 'a))))

(define-test "(list-tabulate)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (cond-expand (immutable-pairs
    (ann (list-tabulate 4 *) (Listof <number>))))

  (assert-equal '(0 1 2 3) (list-tabulate 4 values))

  (assert-equal '("" "*" "**" "***") (list-tabulate 4 (lambda (n) (make-string n #\*))))))

(define-test "(drop)" (expect-success
  (import (llambda list))

  ; Because (drop) works on non-lists it isn't polymorphic. (list-tail) can be used instead which is identical to (drop)
  ; but only works on lists
  (assert-equal '(c d e) (drop '(a b c d e)  2))
  (assert-equal '(3 . d) (drop '(1 2 3 . d) 2))
  (assert-equal 'd (drop '(1 2 3 . d) 3))
  (assert-equal 'foo (drop 'foo 0))))

(define-test "(drop) past end of list fails" (expect-error range-error?
  (import (llambda list))
  (drop '(1 2 3) 4)))

(define-test "(take)" (expect-success
  (import (llambda list))

  (assert-equal '(a b) (take '(a b c d e) 2))
  (assert-equal '(1 2) (take '(1 2 3 . d) 2))
  (assert-equal '(1 2 3) (take '(1 2 3 . d) 3))
  (assert-equal '() (take 'foo 0))))

(define-test "(take) past end of list fails" (expect-error range-error?
  (import (llambda list))
  (take '(1 2 3) 4)))

(define-test "(split-at)" (expect-success
  (import (llambda list))

  (let-values (((head tail) (split-at '(a b c d e f g h) 3)))
              (assert-equal '(a b c) head)
              (assert-equal '(d e f g h) tail))

  (let-values (((head tail) (split-at '(1 2 3 . d) 2)))
              (assert-equal '(1 2) head)
              (assert-equal '(3 . d) tail))

  (let-values (((head tail) (split-at '(1 2 3 . d) 3)))
              (assert-equal '(1 2 3) head)
              (assert-equal 'd tail))

  (let-values (((head tail) (split-at 'foo 0)))
              (assert-equal '() head)
              (assert-equal 'foo tail))))

(define-test "(split-at) past end of list fails" (expect-error range-error?
  (import (llambda list))
  (split-at '(1 2 3) 4)))

(define-test "(span)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (let-values (((head tail) (span even? '(2 18 3 10 22 9))))
              (cond-expand (immutable-pairs
                             (ann head (Listof <exact-integer>))
                             (ann tail (Listof <exact-integer>))))

              (assert-equal '(2 18) head)
              (assert-equal '(3 10 22 9) tail))

  (let-values (((head tail) (span symbol? '(2 18 3 10 22 9))))
              (assert-equal '() head)
              (assert-equal '(2 18 3 10 22 9) tail))

  (let-values (((head tail) (span even? '())))
              (assert-equal '() head)
              (assert-equal '() tail))))

(define-test "(span) with improper list fails" (expect-error type-error?
  (import (llambda list))
  (span even? '(2 18 3 10 22 . 9))))

(define-test "(break)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (let-values (((head tail) (break even? '(3 1 4 1 5 9))))
              (cond-expand (immutable-pairs
                             (ann head (Listof <exact-integer>))
                             (ann tail (Listof <exact-integer>))))

              (assert-equal '(3 1) head)
              (assert-equal '(4 1 5 9) tail))

  (let-values (((head tail) (break symbol? '(3 1 4 1 5 9))))
              (assert-equal '(3 1 4 1 5 9) head)
              (assert-equal '() tail))

  (let-values (((head tail) (break even? '())))
              (assert-equal '() head)
              (assert-equal '() tail))))

(define-test "(break) with improper list fails" (expect-error type-error?
  (import (llambda list))
  (break even? '(2 18 3 10 22 . 9))))

(define-test "(any)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (assert-equal #t (ann (any integer? '(a 3 b 2.7)) <boolean>))
  (assert-equal #f (ann (any integer? '(a 3.1 b 2.7)) <boolean>))
  (assert-equal #f (any integer? '()))

  (assert-equal #t (any < '(3 1 4 1 5) '(2 7 1 8 2)))
  (assert-equal #f (any < '(3 8 4 8 5) '(2 7 1 8 2)))

  (assert-equal #f (any string->number '("one" "two" "three" "four")))
  (assert-equal 10 (any string->number '("one" "two" "three" "10")))
  (assert-equal 16 (any string->number '("one" "two" "three" "10") '(2 8 10 16)))

  (cond-expand
    ((not immutable-pairs)
     (begin
       (define input-list (list-copy '(1 2 3 4)))

       (define (pred-proc n)
         ; Mutate the list during (map) - this is an undefined operation but shouldn't crash
         (set-cdr! (cdr input-list) '())
         #f)

       (guard (condition
                (else 'ignore))
              (any pred-proc input-list)))))))

(define-test "(any) with improper list fails" (expect-error type-error?
  (import (llambda list))
  (any even? '(2 18 3 10 22 . 9))))

(define-test "(every)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (assert-equal #t (ann (every integer? '(1 2 3 4)) <boolean>))
  (assert-equal #f (ann (every integer? '(1 2 3 4.1)) <boolean>))
  (assert-equal #t (every integer? '()))

  (assert-equal #t (every < '(1 2 3 4 5) '(6 7 8 9 10)))
  (assert-equal #f (every < '(1 2 3 4 5) '(6 7 8 9 10) '(11 12 13 14 0)))

  (assert-equal #f (every string->number '("10" "10" "10" "ten")))
  (assert-equal 10 (every string->number '("10" "10" "10" "10")))
  (assert-equal 16 (every string->number '("10" "10" "10" "10") '(2 8 10 16)))

  (cond-expand
    ((not immutable-pairs)
     (begin
       (define input-list (list-copy '(1 2 3 4)))

       (define (pred-proc n)
         ; Mutate the list during (map) - this is an undefined operation but shouldn't crash
         (set-cdr! (cdr input-list) '())
         #t)

       (guard (condition
                (else 'ignore))
              (every pred-proc input-list)))))))

(define-test "(every) with improper list fails" (expect-error type-error?
  (import (llambda list))
  (every even? '(2 18 3 10 22 . 9))))

(define-test "(count)" (expect-success
  (import (llambda list))
  (import (llambda typed))

  (assert-equal 3 (count even? '(3 1 4 1 5 9 2 5 6)))
  (assert-equal 0 (count even? '()))

  (assert-equal 3 (count < '(1 2 4 8) '(2 4 6 8 10 12 14 16)))
  (assert-equal 5 (count < '(1 2 3 4 5) '(6 7 8 9 10)))

  (assert-equal 3 (count string->number '("10" "10" "10" "ten")))
  (assert-equal 4 (count string->number '("10" "10" "10" "10")))
  (assert-equal 4 (count string->number '("10" "10" "10" "10") '(2 8 10 16)))

  (cond-expand
    ((not immutable-pairs)
     (begin
       (define input-list (list-copy '(1 2 3 4)))

       (define (pred-proc n)
         ; Mutate the list during (map) - this is an undefined operation but shouldn't crash
         (set-cdr! (cdr input-list) '())
         #t)

       (guard (condition
                (else 'ignore))
              (count pred-proc input-list)))))))

(define-test "(count) with improper list fails" (expect-error type-error?
  (import (llambda list))
  (count even? '(2 18 3 10 22 . 9))))

(define-test "(iota)" (expect-success
  (import (llambda list))

  (assert-equal '() (iota 0))
  (assert-equal '() (iota 0 5))
  (assert-equal '() (iota 0 5 -1))

  (assert-equal '(0 1 2 3 4) (iota 5))
  (assert-equal '(5 6 7 8 9) (iota 5 5))
  (assert-equal '(5.0 6.0 7.0 8.0 9.0) (iota 5 5.0))
  (assert-equal '(5 4 3 2 1) (iota 5 5 -1))
  (assert-equal '(5.0 4.0 3.0 2.0 1.0) (iota 5 5 -1.0))))

(cond-expand
  ((not immutable-pairs)
   (define-test "(list-set!)" (expect (one two three)
     (let ((ls (list 'one 'two 'five)))
       (list-set! ls 2 'three)
       ls)))

   (define-test "(list-set!) past end of list fails" (expect-error range-error?
     (let ((ls (list 'one 'two 'five)))
       (list-set! ls 5 'three)
       ls)))

   (define-test "(list-set!) on constant list fails" (expect-error mutate-literal-error?
     (list-set! '(0 1 2) 1  "oops")))))
