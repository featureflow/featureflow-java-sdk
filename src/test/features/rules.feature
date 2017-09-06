Feature: Rules
  Scenario: Test that the default rule returns a true match
    Given the rule is a default rule
    When the rule is matched against the user
    Then the result from the match should be true

  Scenario Outline: Test that the rule user matching works (User {<attribute>: <attributeValue>} with operator: <operator>, target: <target>, values: <values>, result: <result>)
    Given the user attributes are
      | attribute       | value            |
      | <attribute>     | <attributeValue> |
    And the rule's audience conditions are
      | operator        | target            | values          |
      | <operator>      | <target>          | <values>        |
    When the rule is matched against the user
    Then the result from the match should be <result>
    Examples:
    | attribute       | attributeValue    | operator | target   | values            | result |
      | role            | "beta"            | equals   | role     | ["beta"]          | true   |
      | role            | "alpha"           | equals   | role     | ["beta"]          | false  |
      | role            | ["beta", "alpha"] | equals   | role     | ["beta"]          | true   |
      | role            | ["beta", "alpha"] | equals   | role     | ["alpha"]         | true   |
      | role            | ["beta", "alpha"] | equals   | role     | ["nope"]          | false  |

  Scenario: Test multiple conditions all passing will return true
    Given the user attributes are
      | attribute       | value   |
      | role            | "beta"           |
      | account         | "premium"        |
    And the rule's audience conditions are
      | operator        | target            | values          |
      | equals          | role              | ["beta"]        |
      | equals          | account           | ["premium"]     |
    When the rule is matched against the user
    Then the result from the match should be true

  Scenario: Test one conditions, but one failing, will return false
    Given the user attributes are
      | attribute       | value   |
      | role            | "beta"           |
      | account         | "premium"        |
    And the rule's audience conditions are
      | operator        | target            | values          |
      | equals          | role              | ["beta"]        |
      | equals          | account           | ["premium"]     |
      | equals          | notInUser      | ["not here"]    |
    When the rule is matched against the user
    Then the result from the match should be false

  Scenario Outline: Get the variant split key works for a default case (value: <value>, on: <on>, off: <off>, result: <result>)
    Given the variant value of <value>
    And the variant splits are
      | variantKey | split  |
      | off        | <off>  |
      | on         | <on>   |
    When the variant split key is calculated
    Then the resulting variant should be "<result>"
    Examples:
      | value | off | on  | result |
      | 50    | 100 | 0   | off    |
      | 50    | 0   | 100 | on     |
      | 11    | 10  | 90  | on     |
      | 9     | 10  | 90  | off    |

  Scenario Outline: Testing multiple splits to make sure the right values work (value: <value>, result: <result>)
    Given the variant value of <value>
    And the variant splits are
      | variantKey | split  |
      | off        | 0      |
      | on         | 30     |
      | alpha      | 30     |
      | alphav2    | 0      |
      | beta       | 30     |
      | betav2     | 10     |
    When the variant split key is calculated
    Then the resulting variant should be "<result>"
    Examples:
      | value | result |
      | 1     | on     |
      | 30    | on     |
      | 50    | alpha  |
      | 61    | beta   |
      | 100   | betav2 |