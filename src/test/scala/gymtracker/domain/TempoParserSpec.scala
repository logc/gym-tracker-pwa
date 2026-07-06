package gymtracker.domain

import org.scalatest.funsuite.AnyFunSuite

class TempoParserSpec extends AnyFunSuite:
  test("parses tempo strings"):
    assert(TempoParser.parse("3-1-2") == Right(Tempo(3, 1, 2)))

  test("rejects malformed tempo strings"):
    assert(TempoParser.parse("fast").isLeft)
    assert(TempoParser.parse("3--2").isLeft)
