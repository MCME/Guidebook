# Coding standards

Judgement calls for review. Formatting is Spotless's job; domain words are in [`GLOSSARY.md`](GLOSSARY.md).

## Comments and Javadoc

- Say why, in words that stay true when the code changes: point at a constant or glossary term instead of restating
  its value or its rules.
- State each rule once (the glossary or the code) and refer to it elsewhere.
- An interface's Javadoc opens with a noun phrase saying what an implementation is ("A way of …"), then the contract
  an implementer must keep. It never names the classes that call it.
- Define each type on its own terms, so no two definitions point at each other.
- Give the real reason ("so tests can use fake Areas"), not a stand-in ("without a running server").

## Names

- Name a module after the role it plays (Watchman, DisplaySwitchboard), checked against the glossary.
- A new name differs clearly from every glossary term: no shared stem like Welcome/Welcoming, and no plural unless the
  class is a plain collection.
