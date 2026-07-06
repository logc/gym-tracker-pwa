package gymtracker.domain

object DefaultMachines:
  val version: Int = 2

  val all: List[Machine] =
    List(
      default("eriksdal-north-abdominal-15", "Abdominal", note("North side", Some("15"), Some(3))),
      default("eriksdal-north-triceps-press-21", "Triceps press", note("North side", Some("21"), Some(1))),
      default("eriksdal-north-arm-extension-19", "Arm extension", note("North side", Some("19"), Some(1))),
      default("eriksdal-north-standing-calf-22", "Standing calf", note("North side", Some("22"), Some(1))),
      default("eriksdal-north-prone-leg-curl-4", "Prone leg curl", note("North side", Some("4"), Some(2))),
      default("eriksdal-north-glute-3", "Glute", note("North side", Some("3"), Some(1))),
      default("eriksdal-north-arm-curl-20", "Arm curl", note("North side", Some("20"), Some(1))),
      default("eriksdal-north-overhead-press-99", "Overhead press", note("North side", Some("99"), Some(2))),
      default("eriksdal-north-lateral-raise-14", "Lateral raise", note("North side", Some("14"), Some(1))),
      default("eriksdal-north-row-14", "Row", note("North side", Some("14"), Some(3))),
      default("eriksdal-north-pulldown-12", "Pulldown", note("North side", Some("12"), Some(2))),
      default("eriksdal-north-seated-leg-curl-6", "Seated leg curl", note("North side", Some("6"), Some(2))),
      default("eriksdal-north-hip-abduction-2", "Hip abduction", note("North side", Some("2"), Some(2))),
      default("eriksdal-north-leg-press-1", "Leg press", note("North side", Some("1"), Some(4))),
      default("eriksdal-north-leg-extension-5", "Leg extension", note("North side", Some("5"), Some(3))),
      default("eriksdal-north-lat-pull-7", "Lat pull", note("North side", Some("7"), Some(1))),
      default("eriksdal-north-row-rear-delt-8", "Row/rear delt", note("North side", Some("8"), Some(1))),
      default("eriksdal-north-chest-press-9", "Chest press", note("North side", Some("9"), Some(1))),
      default("eriksdal-north-overhead-press-10", "Overhead press", note("North side", Some("10"), Some(1))),
      default("eriksdal-north-tricepspress", "Tricepspress", note("North side", None, Some(1))),
      default("eriksdal-north-ryggsmaskin", "Ryggsmaskin", note("North side", None, Some(1))),
      default("eriksdal-east-dual-handle-lat-pull", "Dual handle lat pull", note("East side station", None, Some(2))),
      default("eriksdal-east-hi-lo-cable", "Hi-lo cable", note("East side station", None, Some(2))),
      default("eriksdal-east-row", "Row", note("East side station", None, Some(2))),
      default("eriksdal-east-adjustable-cable", "Adjustable cable", note("East side station", None, Some(2))),
      default("eriksdal-east-dip-chin-assist-11", "Dip/chin assist", note("East side", Some("11"), Some(2))),
      default("eriksdal-east-bench-press", "Bench press", note("East side", None, Some(2))),
      default("eriksdal-east-fly-rear-delt-16", "Fly/rear delt", note("East side", Some("16"), Some(2))),
      default("eriksdal-east-chest-press-18", "Chest press", note("East side", Some("18"), Some(2))),
      default("eriksdal-east-multi-press", "Multi-press", note("East side", None, Some(1))),
      default("eriksdal-east-cables-pulleys", "Other cables and pulleys", note("East side", None, None))
    )

  private def default(id: String, name: String, note: String): Machine =
    MachineClassifier.machineFromName(id, name).copy(notes = Some(note))

  private def note(area: String, visibleId: Option[String], quantity: Option[Int]): String =
    val idPart = visibleId.map(id => s"Visible ID: $id").getOrElse("Visible ID: unknown")
    val quantityPart = quantity.map(count => s"Quantity: $count").getOrElse("Quantity: unknown")
    s"Eriksdalbadet $area · Cybex Prestige · $idPart · $quantityPart"
