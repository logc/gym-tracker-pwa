package gymtracker.domain

object TrainingDefaults:
  val majorMuscleGroups: List[MuscleGroup] =
    List(
      MuscleGroup.Chest,
      MuscleGroup.Back,
      MuscleGroup.Shoulders,
      MuscleGroup.Biceps,
      MuscleGroup.Triceps,
      MuscleGroup.Quads,
      MuscleGroup.Hamstrings,
      MuscleGroup.Glutes,
      MuscleGroup.Calves,
      MuscleGroup.Abs,
      MuscleGroup.LowerBack
    )

  private val push = List(MuscleGroup.Chest, MuscleGroup.Shoulders, MuscleGroup.Triceps)
  private val pull = List(MuscleGroup.Back, MuscleGroup.Biceps, MuscleGroup.LowerBack)
  private val legs = List(MuscleGroup.Quads, MuscleGroup.Hamstrings, MuscleGroup.Glutes, MuscleGroup.Calves)
  private val core = List(MuscleGroup.Abs, MuscleGroup.LowerBack)

  val weeklySplits: Map[Int, List[List[MuscleGroup]]] =
    Map(
      2 -> List(push ++ pull, legs ++ core),
      3 -> List(push, legs, pull ++ List(MuscleGroup.Abs)),
      4 -> List(push, pull, legs, core ++ List(MuscleGroup.Glutes)),
      5 -> List(List(MuscleGroup.Chest, MuscleGroup.Triceps), List(MuscleGroup.Back, MuscleGroup.Biceps), legs, List(MuscleGroup.Shoulders, MuscleGroup.Abs), List(MuscleGroup.Glutes, MuscleGroup.LowerBack, MuscleGroup.Calves)),
      6 -> List(List(MuscleGroup.Chest, MuscleGroup.Triceps), List(MuscleGroup.Back, MuscleGroup.Biceps), legs, List(MuscleGroup.Shoulders, MuscleGroup.Abs), pull, legs ++ core),
      7 -> List(List(MuscleGroup.Chest), List(MuscleGroup.Back), List(MuscleGroup.Quads, MuscleGroup.Glutes), List(MuscleGroup.Shoulders), List(MuscleGroup.Biceps, MuscleGroup.Triceps), List(MuscleGroup.Hamstrings, MuscleGroup.Calves), core)
    )

  val levelDefaults: Map[FitnessLevel, LevelDefaults] =
    Map(
      FitnessLevel.Beginner -> LevelDefaults(sets = 2, reps = RepRange(10, 12), tempo = Tempo(2, 0, 2), restSeconds = 75, exerciseCount = 4),
      FitnessLevel.Intermediate -> LevelDefaults(sets = 3, reps = RepRange(8, 12), tempo = Tempo(3, 0, 2), restSeconds = 90, exerciseCount = 5),
      FitnessLevel.Advanced -> LevelDefaults(sets = 4, reps = RepRange(6, 10), tempo = Tempo(3, 1, 2), restSeconds = 120, exerciseCount = 6)
    )

  def targetsFor(preferences: WorkoutPreferences): List[MuscleGroup] =
    val normalized = preferences.normalized
    val split = weeklySplits.getOrElse(normalized.daysPerWeek, weeklySplits(3))
    split(normalized.sessionNumber - 1)

  def targetsFor(day: Weekday): List[MuscleGroup] =
    val session = day match
      case Weekday.Monday | Weekday.Thursday => 1
      case Weekday.Tuesday | Weekday.Friday  => 2
      case _                                 => 3
    targetsFor(WorkoutPreferences(3, session, 45))

  def defaultsFor(level: FitnessLevel): LevelDefaults =
    levelDefaults(level)
