Feature: Hashing Algorithm
  Scenario Outline: Testing that the key "<key>", salt "<salt>" and feature "<feature>" returns the result "<result>"
    Given the salt is "<salt>", the feature is "<feature>" and the key is "<key>"
    When the variant value is calculated
    Then the hash value calculated should equal "<hash>"
    And the result from the variant calculation should be <result>


    Examples:
      | salt  | feature   | key        | result | hash            |
      | 1     | f1        | alice      | 9      | de5ce0fbc583fd8 |
      | 1     | f1        | bob        | 14     | 8ecddc9f392dc35 |
      | 2     | f1        | alice      | 71     | e31eff9e88214f2 |
      | 2     | f1        | bob        | 58     | 591e96e46fc1dad |
      | 3     | f1        | alice      | 36     | 05ad8a286f0b0bb |
      | 3     | f1        | bob        | 2      | 9bc2af62801255d |