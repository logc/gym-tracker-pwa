package gymtracker.domain

object MachineClassifier:
  private final case class Rule(
      keywords: List[String],
      primary: List[MuscleGroup],
      secondary: List[MuscleGroup],
      pattern: MovementPattern
  )

  private val rules: List[Rule] =
    List(
      Rule(List("chest press", "bench press", "pec press"), List(MuscleGroup.Chest), List(MuscleGroup.Triceps, MuscleGroup.Shoulders), MovementPattern.HorizontalPush),
      Rule(List("shoulder press", "overhead press"), List(MuscleGroup.Shoulders), List(MuscleGroup.Triceps), MovementPattern.VerticalPush),
      Rule(List("lat pulldown", "pulldown", "pull down"), List(MuscleGroup.Back), List(MuscleGroup.Biceps), MovementPattern.VerticalPull),
      Rule(List("seated row", "row machine", "cable row"), List(MuscleGroup.Back), List(MuscleGroup.Biceps), MovementPattern.HorizontalPull),
      Rule(List("leg press", "hack squat"), List(MuscleGroup.Quads, MuscleGroup.Glutes), List(MuscleGroup.Hamstrings), MovementPattern.LowerPush),
      Rule(List("leg extension"), List(MuscleGroup.Quads), Nil, MovementPattern.Isolation),
      Rule(List("leg curl", "hamstring curl"), List(MuscleGroup.Hamstrings), Nil, MovementPattern.KneeFlexion),
      Rule(List("hip thrust", "glute drive", "glute"), List(MuscleGroup.Glutes), List(MuscleGroup.Hamstrings), MovementPattern.HipExtension),
      Rule(List("calf raise", "calf press", "standing calf"), List(MuscleGroup.Calves), Nil, MovementPattern.Isolation),
      Rule(List("ab crunch", "crunch", "abdominal"), List(MuscleGroup.Abs), Nil, MovementPattern.Core),
      Rule(List("back extension", "hyperextension", "ryggsmaskin"), List(MuscleGroup.LowerBack), List(MuscleGroup.Glutes, MuscleGroup.Hamstrings), MovementPattern.Hinge),
      Rule(List("adductor"), List(MuscleGroup.Adductors), Nil, MovementPattern.Isolation),
      Rule(List("abductor", "abduction"), List(MuscleGroup.Abductors), List(MuscleGroup.Glutes), MovementPattern.Isolation),
      Rule(List("biceps curl", "preacher curl", "arm curl"), List(MuscleGroup.Biceps), Nil, MovementPattern.Isolation),
      Rule(List("triceps press", "tricep press", "tricepspress", "triceps extension", "tricep extension", "arm extension"), List(MuscleGroup.Triceps), Nil, MovementPattern.Isolation),
      Rule(List("lateral raise"), List(MuscleGroup.Shoulders), Nil, MovementPattern.Isolation),
      Rule(List("rear delt", "reverse fly"), List(MuscleGroup.Shoulders, MuscleGroup.Back), Nil, MovementPattern.HorizontalPull),
      Rule(List("pec deck", "fly", "chest fly"), List(MuscleGroup.Chest), List(MuscleGroup.Shoulders), MovementPattern.Isolation),
      Rule(List("lat pull"), List(MuscleGroup.Back), List(MuscleGroup.Biceps), MovementPattern.VerticalPull),
      Rule(List("row"), List(MuscleGroup.Back), List(MuscleGroup.Biceps), MovementPattern.HorizontalPull)
    )

  def classify(name: String): MachineClassification =
    val normalized = name.trim.toLowerCase
    rules.find(rule => rule.keywords.exists(normalized.contains)) match
      case Some(rule) =>
        val muscles = (rule.primary ++ rule.secondary).distinct
        MachineClassification(muscles, rule.primary, rule.secondary, rule.pattern, confidence = 0.9)
      case None =>
        MachineClassification(Nil, Nil, Nil, MovementPattern.Unknown, confidence = 0.0)

  def machineFromName(id: String, name: String): Machine =
    val classification = classify(name)
    Machine(
      id = id,
      name = name.trim,
      muscles = classification.muscles,
      primaryMuscles = classification.primaryMuscles,
      secondaryMuscles = classification.secondaryMuscles,
      movementPattern = classification.movementPattern
    )
