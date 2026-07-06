package gymtracker.app

import gymtracker.domain.*

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

enum Screen:
  case Today, Machines, Routine, Workout, Settings

final case class AppModel(
    machines: List[Machine],
    fitnessLevel: FitnessLevel,
    workoutPreferences: WorkoutPreferences,
    cueMode: CueMode,
    autoStartSets: Boolean,
    workout: Option[PlannedWorkout],
    screen: Screen,
    player: PlayerState,
    defaultCatalogVersion: Int
)

final case class PlayerState(
    exerciseIndex: Int = 0,
    setNumber: Int = 1,
    repNumber: Int = 0,
    phase: String = "ready",
    phaseRemaining: Int = 0,
    running: Boolean = false,
    resting: Boolean = false,
    restRemaining: Int = 0,
    finished: Boolean = false
)

object Main:
  private val storageKey = "gym-routine-generator-v1"
  private var model: AppModel = Storage.load().getOrElse(defaultModel)
  private var intervalHandle: Option[Int] = None
  private var audioContext: Option[dom.AudioContext] = None
  private var phaseQueue: List[(String, Int)] = Nil

  @JSExportTopLevel("main")
  def main(): Unit =
    render()

  private def defaultModel: AppModel =
    AppModel(DefaultMachines.all, FitnessLevel.Intermediate, WorkoutPreferences(3, 1, 45), CueMode.Beep, autoStartSets = false, None, Screen.Today, PlayerState(), DefaultMachines.version)

  private def render(): Unit =
    val root = dom.document.getElementById("app")
    if root != null then
      root.innerHTML = view(model)
      bindEvents()
      Storage.save(model)

  private def setModel(next: AppModel): Unit =
    model = next
    render()

  private def view(model: AppModel): String =
    s"""
      <section class="app-shell">
        <header class="topbar">
          <div>
            <p class="eyebrow">Today</p>
            <h1>Gym Routine</h1>
          </div>
          <button class="icon-button" id="settings-button" aria-label="Settings">⚙</button>
        </header>
        ${tabs(model.screen)}
        <section class="screen">
          ${model.screen match
            case Screen.Today    => todayView(model)
            case Screen.Machines => machinesView(model)
            case Screen.Routine  => routineView(model)
            case Screen.Workout  => workoutView(model)
            case Screen.Settings => settingsView(model)}
        </section>
      </section>
    """

  private def tabs(active: Screen): String =
    val items = List(
      Screen.Today -> "Today",
      Screen.Machines -> "Machines",
      Screen.Routine -> "Routine",
      Screen.Workout -> "Workout"
    )
    s"""<nav class="tabs">${items.map { case (screen, label) =>
        val selected = if screen == active then "active" else ""
        s"""<button class="tab $selected" data-screen="${screen.productPrefix}">$label</button>"""
      }.mkString}</nav>"""

  private def todayView(model: AppModel): String =
    val today = browserToday()
    val preferences = model.workoutPreferences.normalized
    val targets = TrainingDefaults.targetsFor(preferences).map(_.label).mkString(", ")
    val knownCount = model.machines.count(!_.isUnknown)
    s"""
      <div class="panel">
        <p class="label">Session ${preferences.sessionNumber} of ${preferences.daysPerWeek}</p>
        <h2>${escape(targets)}</h2>
        <p>${escape(today.weekday.label.capitalize)} · ${model.fitnessLevel.label} · about ${preferences.targetMinutes} min</p>
      </div>
      <div class="action-grid">
        <button class="primary" id="generate-workout">Generate workout</button>
        <button id="start-workout" ${if model.workout.exists(_.exercises.nonEmpty) then "" else "disabled"}>Start workout</button>
      </div>
      <p class="disclaimer">This app generates general workout suggestions. Use proper form, adjust weight conservatively, and stop if something hurts.</p>
      <div class="stats">
        <span>${model.machines.size} machines</span>
        <span>${knownCount} classified</span>
        <span>${model.workout.map(_.estimatedMinutes).getOrElse(0)} min planned</span>
      </div>
    """

  private def machinesView(model: AppModel): String =
    val cards =
      if model.machines.isEmpty then """<p class="empty">Add the machines available in this gym session.</p>"""
      else model.machines.map(machineCard).mkString

    s"""
      <form class="add-row" id="add-machine-form">
        <input id="machine-name" autocomplete="off" placeholder="Machine name, e.g. Lat pulldown" />
        <button class="primary" type="submit">Add</button>
      </form>
      <div class="machine-list">$cards</div>
    """

  private def machineCard(machine: Machine): String =
    val muscleChecks = MuscleGroup.all.map { muscle =>
      val checked = if machine.muscles.contains(muscle) then "checked" else ""
      s"""<label class="check"><input type="checkbox" data-machine-muscle="${machine.id}" value="${muscle.label}" $checked /> ${muscle.label}</label>"""
    }.mkString
    val patterns = MovementPattern.all.map { pattern =>
      val selected = if pattern == machine.movementPattern then "selected" else ""
      s"""<option value="${pattern.label}" $selected>${pattern.label}</option>"""
    }.mkString
    val warning = if machine.isUnknown then """<p class="warning">Needs manual classification before routine generation.</p>""" else ""
    val notes = machine.notes.map(value => s"""<p class="machine-note">${escape(value)}</p>""").getOrElse("")
    s"""
      <article class="card">
        <div class="card-head">
          <div>
            <h3>${escape(machine.name)}</h3>
            <p>${escape(machine.movementPattern.label)}</p>
          </div>
          <button class="danger ghost" data-delete-machine="${machine.id}">Delete</button>
        </div>
        $notes
        $warning
        <div class="checkbox-grid">$muscleChecks</div>
        <label class="field">Movement
          <select data-machine-pattern="${machine.id}">$patterns</select>
        </label>
        <button data-save-machine="${machine.id}">Save classification</button>
      </article>
    """

  private def routineView(model: AppModel): String =
    val body = model.workout match
      case None =>
        """<p class="empty">Generate a workout from the Today screen.</p>"""
      case Some(workout) if workout.exercises.isEmpty =>
        """<p class="empty">No classified machines match yet. Add or classify machines first.</p>"""
      case Some(workout) =>
        val summary = s"""<div class="panel"><p class="label">Estimated duration</p><h2>${workout.estimatedMinutes} min</h2><p>Target: ${workout.preferences.targetMinutes} min · Session ${workout.preferences.sessionNumber} of ${workout.preferences.daysPerWeek}</p></div>"""
        summary + workout.exercises.map(exerciseCard).mkString +
          """<div class="action-grid"><button class="primary" id="regenerate-workout">Regenerate</button><button id="routine-start-workout">Start workout</button></div>"""
    s"""<div class="routine-list">$body</div>"""

  private def exerciseCard(exercise: PlannedExercise): String =
    val metrics =
      exercise.durationMinutes match
        case Some(minutes) => s"""<span>${minutes} min</span><span>timed</span>"""
        case None          => s"""<span>${exercise.sets} sets</span><span>${exercise.reps} reps</span><span>${exercise.tempo}</span><span>${exercise.restSeconds}s rest</span>"""
    val replace =
      if exercise.durationMinutes.isDefined then ""
      else s"""<button data-replace-exercise="${exercise.id}">Replace</button>"""
    s"""
      <article class="card exercise-card">
        <div>
          <h3>${escape(exercise.machineName)}</h3>
          <p>${exercise.muscles.map(_.label).mkString(", ")} · ${exercise.movementPattern.label}</p>
        </div>
        <div class="pill-row">
          $metrics
        </div>
        $replace
      </article>
    """

  private def workoutView(model: AppModel): String =
    model.workout match
      case None =>
        """<p class="empty">Generate a workout first.</p>"""
      case Some(workout) if workout.exercises.isEmpty =>
        """<p class="empty">Your generated routine is empty.</p>"""
      case Some(workout) =>
        val player = model.player
        val exercise = workout.exercises(math.min(player.exerciseIndex, workout.exercises.size - 1))
        val status =
          if player.finished then "Workout complete"
          else if player.resting then s"Rest ${player.restRemaining}s"
          else if player.running && exercise.isTimed then s"${player.phaseRemaining}s remaining"
          else if player.running then s"${player.phase} · ${player.phaseRemaining}s"
          else "Ready"
        s"""
          <section class="player">
            <p class="label">Exercise ${player.exerciseIndex + 1} of ${workout.exercises.size}</p>
            <h2>${escape(exercise.machineName)}</h2>
            <p>${exercise.muscles.map(_.label).mkString(", ")}</p>
            ${playerMeter(player, exercise)}
            <div class="phase">${escape(status)}</div>
            <div class="action-grid">
              <button class="primary" id="start-set" ${if player.running || player.resting || player.finished then "disabled" else ""}>Start set</button>
              <button id="pause-player" ${if player.running then "" else "disabled"}>Pause</button>
              <button id="finish-set" ${if player.finished then "disabled" else ""}>Finish set</button>
              <button id="next-exercise" ${if player.finished then "disabled" else ""}>Next exercise</button>
            </div>
          </section>
        """

  private def playerMeter(player: PlayerState, exercise: PlannedExercise): String =
    exercise.durationMinutes match
      case Some(minutes) =>
        s"""
          <div class="player-meter">
            <span>${minutes} min block</span>
            <span>${if player.running then player.phaseRemaining.toString + "s" else "Ready"}</span>
          </div>
        """
      case None =>
        s"""
          <div class="player-meter">
            <span>Set ${player.setNumber}/${exercise.sets}</span>
            <span>Rep ${player.repNumber}/${exercise.reps}</span>
          </div>
        """

  private def settingsView(model: AppModel): String =
    val levelOptions = FitnessLevel.all.map { level =>
      val selected = if level == model.fitnessLevel then "selected" else ""
      s"""<option value="${level.label}" $selected>${level.label}</option>"""
    }.mkString
    val cueOptions = CueMode.all.map { mode =>
      val selected = if mode == model.cueMode then "selected" else ""
      s"""<option value="${mode.label}" $selected>${mode.label}</option>"""
    }.mkString
    val auto = if model.autoStartSets then "checked" else ""
    val preferences = model.workoutPreferences.normalized
    val dayOptions = (2 to 7).map { days =>
      val selected = if days == preferences.daysPerWeek then "selected" else ""
      s"""<option value="$days" $selected>$days days/week</option>"""
    }.mkString
    val sessionOptions = (1 to preferences.daysPerWeek).map { session =>
      val selected = if session == preferences.sessionNumber then "selected" else ""
      s"""<option value="$session" $selected>Session $session</option>"""
    }.mkString
    val durationOptions = List(30, 45, 60, 75, 90).map { minutes =>
      val selected = if minutes == preferences.targetMinutes then "selected" else ""
      s"""<option value="$minutes" $selected>$minutes minutes</option>"""
    }.mkString
    s"""
      <div class="panel">
        <label class="field">Fitness level
          <select id="fitness-level">$levelOptions</select>
        </label>
        <label class="field">Training frequency
          <select id="training-days">$dayOptions</select>
        </label>
        <label class="field">Workout to generate
          <select id="session-number">$sessionOptions</select>
        </label>
        <label class="field">Target duration
          <select id="target-minutes">$durationOptions</select>
        </label>
        <label class="field">Cue mode
          <select id="cue-mode">$cueOptions</select>
        </label>
        <label class="toggle"><input type="checkbox" id="auto-start" $auto /> Auto-start next set after rest</label>
      </div>
    """

  private def bindEvents(): Unit =
    onClick("settings-button")(_ => setModel(model.copy(screen = Screen.Settings)))
    dom.document.querySelectorAll("[data-screen]").foreach { node =>
      node.addEventListener("click", _ =>
        val target = node.asInstanceOf[dom.html.Element].getAttribute("data-screen")
        Screen.values.find(_.productPrefix == target).foreach(screen => setModel(model.copy(screen = screen)))
      )
    }
    Option(dom.document.getElementById("add-machine-form")).foreach { form =>
      form.addEventListener("submit", event =>
        event.preventDefault()
        val input = dom.document.getElementById("machine-name").asInstanceOf[dom.html.Input]
        val name = input.value.trim
        if name.nonEmpty then
          val machine = MachineClassifier.machineFromName(newId(), name)
          setModel(model.copy(machines = model.machines :+ machine))
      )
    }
    onClick("generate-workout")(_ => generateWorkout())
    onClick("regenerate-workout")(_ => generateWorkout())
    onClick("start-workout")(_ => setModel(model.copy(screen = Screen.Workout, player = PlayerState())))
    onClick("routine-start-workout")(_ => setModel(model.copy(screen = Screen.Workout, player = PlayerState())))
    onClick("start-set")(_ => startSet())
    onClick("pause-player")(_ => pausePlayer())
    onClick("finish-set")(_ => finishSet())
    onClick("next-exercise")(_ => nextExercise())
    bindMachineControls()
    bindRoutineControls()
    bindSettings()

  private def bindMachineControls(): Unit =
    dom.document.querySelectorAll("[data-delete-machine]").foreach { node =>
      node.addEventListener("click", _ =>
        val id = node.asInstanceOf[dom.html.Element].getAttribute("data-delete-machine")
        setModel(model.copy(machines = model.machines.filterNot(_.id == id)))
      )
    }
    dom.document.querySelectorAll("[data-save-machine]").foreach { node =>
      node.addEventListener("click", _ =>
        val id = node.asInstanceOf[dom.html.Element].getAttribute("data-save-machine")
        val muscles = checkedMuscles(id)
        val patternValue = dom.document.querySelector(s"""[data-machine-pattern="$id"]""").asInstanceOf[dom.html.Select].value
        val pattern = MovementPattern.fromLabel(patternValue).getOrElse(MovementPattern.Unknown)
        setModel(model.copy(machines = model.machines.map {
          case machine if machine.id == id =>
            machine.copy(muscles = muscles, primaryMuscles = muscles.take(1), secondaryMuscles = muscles.drop(1), movementPattern = pattern)
          case machine => machine
        }))
      )
    }

  private def bindRoutineControls(): Unit =
    dom.document.querySelectorAll("[data-replace-exercise]").foreach { node =>
      node.addEventListener("click", _ =>
        val id = node.asInstanceOf[dom.html.Element].getAttribute("data-replace-exercise")
        model.workout.foreach(workout => setModel(model.copy(workout = Some(WorkoutGenerator.replaceExercise(workout, id, model.machines)))))
      )
    }

  private def bindSettings(): Unit =
    Option(dom.document.getElementById("fitness-level")).foreach { node =>
      node.addEventListener("change", _ =>
        val selected = node.asInstanceOf[dom.html.Select].value
        FitnessLevel.fromLabel(selected).foreach(level => setModel(model.copy(fitnessLevel = level)))
      )
    }
    Option(dom.document.getElementById("cue-mode")).foreach { node =>
      node.addEventListener("change", _ =>
        val selected = node.asInstanceOf[dom.html.Select].value
        CueMode.fromLabel(selected).foreach(mode => setModel(model.copy(cueMode = mode)))
      )
    }
    Option(dom.document.getElementById("training-days")).foreach { node =>
      node.addEventListener("change", _ =>
        val days = node.asInstanceOf[dom.html.Select].value.toIntOption.getOrElse(model.workoutPreferences.daysPerWeek)
        val nextPreferences = model.workoutPreferences.copy(daysPerWeek = days).normalized
        setModel(model.copy(workoutPreferences = nextPreferences))
      )
    }
    Option(dom.document.getElementById("session-number")).foreach { node =>
      node.addEventListener("change", _ =>
        val session = node.asInstanceOf[dom.html.Select].value.toIntOption.getOrElse(model.workoutPreferences.sessionNumber)
        setModel(model.copy(workoutPreferences = model.workoutPreferences.copy(sessionNumber = session).normalized))
      )
    }
    Option(dom.document.getElementById("target-minutes")).foreach { node =>
      node.addEventListener("change", _ =>
        val minutes = node.asInstanceOf[dom.html.Select].value.toIntOption.getOrElse(model.workoutPreferences.targetMinutes)
        setModel(model.copy(workoutPreferences = model.workoutPreferences.copy(targetMinutes = minutes).normalized))
      )
    }
    Option(dom.document.getElementById("auto-start")).foreach { node =>
      node.addEventListener("change", _ =>
        setModel(model.copy(autoStartSets = node.asInstanceOf[dom.html.Input].checked))
      )
    }

  private def checkedMuscles(machineId: String): List[MuscleGroup] =
    dom.document.querySelectorAll(s"""[data-machine-muscle="$machineId"]""").toList.flatMap { node =>
      val input = node.asInstanceOf[dom.html.Input]
      if input.checked then MuscleGroup.fromLabel(input.value) else None
    }

  private def generateWorkout(): Unit =
    val workout = WorkoutGenerator.generate(model.machines, browserToday(), model.fitnessLevel, model.workoutPreferences)
    setModel(model.copy(workout = Some(workout), screen = Screen.Routine, player = PlayerState()))

  private def startSet(): Unit =
    ensureAudio()
    currentExercise.foreach { exercise =>
      val phases = List("down" -> exercise.tempo.eccentric, "hold" -> exercise.tempo.pause, "up" -> exercise.tempo.concentric).filter(_._2 > 0)
      phaseQueue = List.fill(exercise.reps)(phases).flatten
      advancePhase()
      clearTimer()
      intervalHandle = Some(dom.window.setInterval(() => tick(), 1000))
    }

  private def tick(): Unit =
    if model.player.resting then
      val nextRemaining = model.player.restRemaining - 1
      if nextRemaining <= 0 then
        clearTimer()
        val next = model.copy(player = model.player.copy(resting = false, restRemaining = 0, phase = "ready"))
        model = next
        if model.autoStartSets then startSet() else render()
      else setModel(model.copy(player = model.player.copy(restRemaining = nextRemaining)))
    else if model.player.running then
      val nextRemaining = model.player.phaseRemaining - 1
      if nextRemaining <= 0 then advancePhase()
      else setModel(model.copy(player = model.player.copy(phaseRemaining = nextRemaining)))

  private def advancePhase(): Unit =
    phaseQueue match
      case Nil => finishSet()
      case (phase, seconds) :: rest =>
        phaseQueue = rest
        val repIncrement = if phase == "down" then 1 else 0
        cue(phase)
        setModel(model.copy(player = model.player.copy(running = true, phase = phase, phaseRemaining = seconds, repNumber = model.player.repNumber + repIncrement)))

  private def finishSet(): Unit =
    clearTimer()
    cue("done")
    currentExercise match
      case None => setModel(model.copy(player = model.player.copy(finished = true, running = false)))
      case Some(exercise) if model.player.setNumber >= exercise.sets =>
        nextExercise()
      case Some(exercise) =>
        setModel(model.copy(player = model.player.copy(running = false, resting = true, restRemaining = exercise.restSeconds, repNumber = 0, setNumber = model.player.setNumber + 1, phase = "rest")))
        intervalHandle = Some(dom.window.setInterval(() => tick(), 1000))

  private def nextExercise(): Unit =
    clearTimer()
    model.workout match
      case Some(workout) if model.player.exerciseIndex + 1 < workout.exercises.size =>
        setModel(model.copy(player = PlayerState(exerciseIndex = model.player.exerciseIndex + 1)))
      case _ =>
        setModel(model.copy(player = model.player.copy(running = false, resting = false, finished = true, phase = "complete")))

  private def pausePlayer(): Unit =
    clearTimer()
    setModel(model.copy(player = model.player.copy(running = false, phase = "paused")))

  private def currentExercise: Option[PlannedExercise] =
    model.workout.flatMap(_.exercises.lift(model.player.exerciseIndex))

  private def ensureAudio(): Unit =
    if audioContext.isEmpty && model.cueMode == CueMode.Beep then
      audioContext = Some(new dom.AudioContext())

  private def cue(label: String): Unit =
    model.cueMode match
      case CueMode.Silent => ()
      case CueMode.Voice =>
        val ctor = js.Dynamic.global.SpeechSynthesisUtterance
        val synthesis = js.Dynamic.global.window.speechSynthesis
        if !js.isUndefined(ctor) && !js.isUndefined(synthesis) then
          synthesis.speak(js.Dynamic.newInstance(ctor)(label))
      case CueMode.Beep =>
        audioContext.foreach { context =>
          val oscillator = context.createOscillator()
          val gain = context.createGain()
          oscillator.frequency.value = if label == "done" then 660 else 440
          gain.gain.value = 0.08
          oscillator.connect(gain)
          gain.connect(context.destination)
          oscillator.start()
          oscillator.stop(context.currentTime + 0.12)
        }

  private def clearTimer(): Unit =
    intervalHandle.foreach(dom.window.clearInterval)
    intervalHandle = None

  private def onClick(id: String)(handler: dom.MouseEvent => Unit): Unit =
    Option(dom.document.getElementById(id)).foreach(_.addEventListener("click", event => handler(event.asInstanceOf[dom.MouseEvent])))

  private def newId(): String =
    s"m-${System.currentTimeMillis()}-${math.abs(scala.util.Random.nextInt())}"

  private def browserToday(): WorkoutDate =
    val date = new js.Date()
    val year = date.getFullYear().toInt
    val month = date.getMonth().toInt + 1
    val day = date.getDate().toInt
    WorkoutDate.fromYmd(year, month, day)

  private def escape(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")

object Storage:
  private val delimiter = "\u001e"
  private val field = "\u001f"

  def save(model: AppModel): Unit =
    val machines = model.machines.map(encodeMachine).mkString(delimiter)
    val workout = model.workout.map(encodeWorkout).getOrElse("")
    val preferences = model.workoutPreferences.normalized
    val value = List(model.fitnessLevel.label, model.cueMode.label, model.autoStartSets.toString, machines, workout, model.defaultCatalogVersion.toString, encodePreferences(preferences)).map(enc).mkString(field)
    dom.window.localStorage.setItem("gym-routine-generator-v1", value)

  def load(): Option[AppModel] =
    Option(dom.window.localStorage.getItem("gym-routine-generator-v1")).flatMap { raw =>
      raw.split(field, -1).toList match
        case level :: cue :: auto :: machinesRaw :: workoutRaw :: versionRaw :: preferencesRaw :: Nil =>
          val machines = dec(machinesRaw).split(delimiter).toList.filter(_.nonEmpty).flatMap(decodeMachine)
          val workout = decodeWorkout(dec(workoutRaw))
          val savedVersion = dec(versionRaw).toIntOption.getOrElse(0)
          val preferences = decodePreferences(dec(preferencesRaw))
          Some(withImportedDefaults(AppModel(
            machines,
            FitnessLevel.fromLabel(dec(level)).getOrElse(FitnessLevel.Intermediate),
            preferences,
            CueMode.fromLabel(dec(cue)).getOrElse(CueMode.Beep),
            dec(auto).toBooleanOption.getOrElse(false),
            workout,
            Screen.Today,
            PlayerState(),
            savedVersion
          )))
        case level :: cue :: auto :: machinesRaw :: workoutRaw :: versionRaw :: Nil =>
          val machines = dec(machinesRaw).split(delimiter).toList.filter(_.nonEmpty).flatMap(decodeMachine)
          val workout = decodeWorkout(dec(workoutRaw))
          val savedVersion = dec(versionRaw).toIntOption.getOrElse(0)
          Some(withImportedDefaults(AppModel(
            machines,
            FitnessLevel.fromLabel(dec(level)).getOrElse(FitnessLevel.Intermediate),
            WorkoutPreferences(3, 1, 45),
            CueMode.fromLabel(dec(cue)).getOrElse(CueMode.Beep),
            dec(auto).toBooleanOption.getOrElse(false),
            workout,
            Screen.Today,
            PlayerState(),
            savedVersion
          )))
        case level :: cue :: auto :: machinesRaw :: workoutRaw :: Nil =>
          val machines = dec(machinesRaw).split(delimiter).toList.filter(_.nonEmpty).flatMap(decodeMachine)
          val workout = decodeWorkout(dec(workoutRaw))
          Some(withImportedDefaults(AppModel(
            machines,
            FitnessLevel.fromLabel(dec(level)).getOrElse(FitnessLevel.Intermediate),
            WorkoutPreferences(3, 1, 45),
            CueMode.fromLabel(dec(cue)).getOrElse(CueMode.Beep),
            dec(auto).toBooleanOption.getOrElse(false),
            workout,
            Screen.Today,
            PlayerState(),
            0
          )))
        case _ => None
    }

  private def withImportedDefaults(model: AppModel): AppModel =
    if model.defaultCatalogVersion >= DefaultMachines.version then model
    else
      val existingIds = model.machines.map(_.id).toSet
      val missingDefaults = DefaultMachines.all.filterNot(machine => existingIds.contains(machine.id))
      model.copy(machines = model.machines ++ missingDefaults, defaultCatalogVersion = DefaultMachines.version)

  private def encodeMachine(machine: Machine): String =
    List(
      machine.id,
      machine.name,
      machine.muscles.map(_.label).mkString(","),
      machine.primaryMuscles.map(_.label).mkString(","),
      machine.secondaryMuscles.map(_.label).mkString(","),
      machine.movementPattern.label,
      machine.notes.getOrElse("")
    ).map(enc).mkString(field)

  private def decodeMachine(raw: String): Option[Machine] =
    raw.split(field, -1).toList match
      case id :: name :: muscles :: primary :: secondary :: pattern :: notes :: Nil =>
        Some(Machine(
          dec(id),
          dec(name),
          decodeMuscles(dec(muscles)),
          decodeMuscles(dec(primary)),
          decodeMuscles(dec(secondary)),
          MovementPattern.fromLabel(dec(pattern)).getOrElse(MovementPattern.Unknown),
          Option(dec(notes)).filter(_.nonEmpty)
        ))
      case _ => None

  private def encodeWorkout(workout: PlannedWorkout): String =
    List(
      workout.id,
      workout.date,
      workout.targetMuscles.map(_.label).mkString(","),
      workout.fitnessLevel.label,
      workout.exercises.map(encodeExercise).mkString(delimiter),
      encodePreferences(workout.preferences),
      workout.estimatedMinutes.toString
    ).map(enc).mkString(field)

  private def decodeWorkout(raw: String): Option[PlannedWorkout] =
    if raw.isEmpty then None
    else
      raw.split(field, -1).toList match
        case id :: date :: targets :: level :: exercises :: preferences :: estimated :: Nil =>
          Some(PlannedWorkout(
            dec(id),
            dec(date),
            decodeMuscles(dec(targets)),
            FitnessLevel.fromLabel(dec(level)).getOrElse(FitnessLevel.Intermediate),
            dec(exercises).split(delimiter).toList.filter(_.nonEmpty).flatMap(decodeExercise),
            decodePreferences(dec(preferences)),
            dec(estimated).toIntOption.getOrElse(0)
          ))
        case id :: date :: targets :: level :: exercises :: Nil =>
          Some(PlannedWorkout(
            dec(id),
            dec(date),
            decodeMuscles(dec(targets)),
            FitnessLevel.fromLabel(dec(level)).getOrElse(FitnessLevel.Intermediate),
            dec(exercises).split(delimiter).toList.filter(_.nonEmpty).flatMap(decodeExercise)
          ))
        case _ => None

  private def encodeExercise(exercise: PlannedExercise): String =
    List(
      exercise.id,
      exercise.machineId,
      exercise.machineName,
      exercise.muscles.map(_.label).mkString(","),
      exercise.movementPattern.label,
      exercise.sets.toString,
      exercise.reps.toString,
      exercise.tempo.toString,
      exercise.restSeconds.toString,
      exercise.durationMinutes.map(_.toString).getOrElse("")
    ).map(enc).mkString("|")

  private def decodeExercise(raw: String): Option[PlannedExercise] =
    raw.split("\\|", -1).toList match
      case id :: machineId :: name :: muscles :: pattern :: sets :: reps :: tempo :: rest :: duration :: Nil =>
        TempoParser.parse(dec(tempo)).toOption.map { parsedTempo =>
          PlannedExercise(
            dec(id),
            dec(machineId),
            dec(name),
            decodeMuscles(dec(muscles)),
            MovementPattern.fromLabel(dec(pattern)).getOrElse(MovementPattern.Unknown),
            dec(sets).toIntOption.getOrElse(3),
            dec(reps).toIntOption.getOrElse(10),
            parsedTempo,
            dec(rest).toIntOption.getOrElse(90),
            dec(duration).toIntOption
          )
        }
      case id :: machineId :: name :: muscles :: pattern :: sets :: reps :: tempo :: rest :: Nil =>
        TempoParser.parse(dec(tempo)).toOption.map { parsedTempo =>
          PlannedExercise(
            dec(id),
            dec(machineId),
            dec(name),
            decodeMuscles(dec(muscles)),
            MovementPattern.fromLabel(dec(pattern)).getOrElse(MovementPattern.Unknown),
            dec(sets).toIntOption.getOrElse(3),
            dec(reps).toIntOption.getOrElse(10),
            parsedTempo,
            dec(rest).toIntOption.getOrElse(90)
          )
        }
      case _ => None

  private def encodePreferences(preferences: WorkoutPreferences): String =
    val normalized = preferences.normalized
    List(normalized.daysPerWeek.toString, normalized.sessionNumber.toString, normalized.targetMinutes.toString).mkString(",")

  private def decodePreferences(raw: String): WorkoutPreferences =
    raw.split(",", -1).toList match
      case days :: session :: minutes :: Nil =>
        WorkoutPreferences(days.toIntOption.getOrElse(3), session.toIntOption.getOrElse(1), minutes.toIntOption.getOrElse(45)).normalized
      case _ => WorkoutPreferences(3, 1, 45)

  private def decodeMuscles(raw: String): List[MuscleGroup] =
    raw.split(",").toList.flatMap(MuscleGroup.fromLabel)

  private def enc(value: String): String =
    js.URIUtils.encodeURIComponent(value)

  private def dec(value: String): String =
    js.URIUtils.decodeURIComponent(value)
