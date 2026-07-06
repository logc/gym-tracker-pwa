package gymtracker.domain

import org.scalatest.funsuite.AnyFunSuite

class DefaultMachinesSpec extends AnyFunSuite:
  test("includes the noted Eriksdalbadet strength machines by default"):
    assert(DefaultMachines.all.exists(machine => machine.name == "Leg press"))
    assert(DefaultMachines.all.exists(machine => machine.name == "Dual handle lat pull"))
    assert(DefaultMachines.all.exists(machine => machine.name == "Ryggsmaskin"))
    assert(DefaultMachines.all.size == 31)

  test("classifies most default machines while leaving cable/multi-use stations for manual correction"):
    val known = DefaultMachines.all.filterNot(_.isUnknown)
    val unknownNames = DefaultMachines.all.filter(_.isUnknown).map(_.name).toSet

    assert(known.size >= 25)
    assert(unknownNames.contains("Hi-lo cable"))
    assert(unknownNames.contains("Adjustable cable"))

  test("stores physical visible IDs and quantities explicitly in notes"):
    val legPress = DefaultMachines.all.find(_.id == "eriksdal-north-leg-press-1").flatMap(_.notes)
    val tricepspress = DefaultMachines.all.find(_.id == "eriksdal-north-tricepspress").flatMap(_.notes)

    assert(legPress.exists(_.contains("Visible ID: 1")))
    assert(legPress.exists(_.contains("Quantity: 4")))
    assert(tricepspress.exists(_.contains("Visible ID: unknown")))
    assert(tricepspress.exists(_.contains("Quantity: 1")))
