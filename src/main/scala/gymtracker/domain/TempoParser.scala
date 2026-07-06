package gymtracker.domain

object TempoParser:
  def parse(value: String): Either[String, Tempo] =
    value.trim.split("-").toList match
      case a :: b :: c :: Nil =>
        for
          eccentric <- parsePart(a, "eccentric")
          pause <- parsePart(b, "pause")
          concentric <- parsePart(c, "concentric")
        yield Tempo(eccentric, pause, concentric)
      case _ =>
        Left("Tempo must use the format 3-1-2")

  private def parsePart(value: String, label: String): Either[String, Int] =
    value.toIntOption.filter(_ >= 0).toRight(s"$label must be a non-negative number")
