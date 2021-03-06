package org.specs2
package matcher

class OptionMatchersSpec extends SpecificationWithJUnit { def is = 
                                                                                          """
  The OptionMatchers trait provides matchers to check Option instances.
                                                                                          """^
                                                                                          p^
  "beSome checks if an element is Some(_)"                                                ^
  { Some(1) must beSome }                                                                 ^ 
  { Some(1) must beSome(1) }                                                              ^ 
  { Some(1) must beSome.which(_ > 0) }                                                    ^ 
  { Some(1) must not be some(2) }                                                         ^ 
  { None must not be some }                                                               ^ 
  { None must not be some(2) }                                                            ^ 
                                                                                          p^
  "beNone checks if an element is None"                                                   ^
  { None must beNone }                                                                    ^
  { Some(1) must not be none }                                                            ^ 
                                                                                          p^
  "beAsNoneAs checks if 2 values are None at the same time"                               ^
  { None must beAsNoneAs(None) }                                                          ^
  { Some(1) must beAsNoneAs(Some(2)) }                                                    ^
                                                                                          end
}