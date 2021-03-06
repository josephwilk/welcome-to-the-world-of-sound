(ns codemash.core
  "HELLO and welcome to the woderful world of SOUND.

  WARNING/DANGER:
    Its possible to generate sounds that may cause harm or damage to
    your ears if  you listen for to long.
    Ears are useful, volume down, avoid ear phones if possible.

  Musical Mission:
    * Create instruments new heard before.
    * Use instruments for greater good.
    * Bend/Manupliate instruments live.
    * Maybe Music...

  Tools:
    * Emacs 24.3
    * Emacs live
      - bash <(curl -fksSL https://raw.github.com/overtone/emacs-live/master/installer/install-emacs-live.sh)
    * Overtone
"
  (:use overtone.live)
  (:require [codemash.art :as art]
            [codemash.voice :as voice]))

(use 'overtone.live)
(odoc demo)

;;;;;;;;;;;;;;;;;;;;;;;;;
;;     Waves           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;
(do (voice/waves) (println art/waves))

(do (demo (sin-osc)) (println art/sin-waves))
(do (demo (lf-saw))  (println art/saw-waves))
(do (demo (lf-tri))  (println art/triangle-waves))
(do (demo (pulse))   (println art/square-waves))
(do (demo (square))  (println art/square-waves))

(stop)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Synthesis                         ;;
;;                                  ;;
;;Turning waves into complex sounds.;;
;; * Additive                       ;;
;; * Subtractive                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(do (voice/synthesis))

(defsynth wallop [])

(show-graphviz-synth wallop)

(def p (wallop))

(ctl p :freq 200 :amp 0.1)
(ctl p :freq 250)
(ctl p :freq 300)
(kill p)
(stop)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;UGENS - Unit generators (Sweet shop);;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; * Cheat sheet:
;;     - https://github.com/overtone/overtone/raw/master/docs/cheatsheet/overtone-cheat-sheet.pdf
(do (voice/ugens) (println art/sweet-shop))

(defsynth woody-beep [freq 300 out-bus 0 dur 0.4]
  (let [tri (* 0.5 (lf-tri:ar freq))
        sin (sin-osc:ar (* 1 freq))
        sin2 (sin-osc:ar (* 1.01 freq))
        wood (bpf:ar (* (white-noise:ar) (line:kr 5 0 0.02)) freq 0.02)
        src (mix [sin sin2 tri wood])
        src (free-verb src)
        env (env-gen:ar (env-perc :release 0.25) :action FREE :time-scale dur)]
    (out out-bus (pan2 (* env src)))))

;; Based on Dawn by Schemawound: http://sccode.org/1-c
(defsynth fallout-wind [decay 30 attack 30 out-bus 0]
  (let [lfo  (+ 0.5 (* 0.5 (sin-osc:kr [(ranged-rand 0.5 1000) (ranged-rand 0.5 1000)] :phase (* 1.5 Math/PI))))
        lfo3 (+ 0.5 (* 0.5 (sin-osc:kr [(ranged-rand 0.1 0.5) (ranged-rand 0.1 0.5)] :phase (* 1.5 Math/PI))))
        lfo2 (+ 0.5 (* 0.5 (sin-osc:kr [(* (ranged-rand 0.5 1000) lfo lfo3) (* (ranged-rand 0.5 1000) (- 1 lfo) (- 1 lfo3))] :phase (* 1.5 Math/PI))))
        fillers (map (fn [_] (* lfo2 (sin-osc:ar (ranged-rand 40 1000) :phase 0))) (range 0 100))]
    (out:ar out-bus  (* (mix:ar fillers)
                        (env-gen:kr (perc attack decay) :action FREE)))))

(def fallout-w (fallout-wind))

(ctl fallout-w :attack 1 :decay 1)

(woody-beep :freq 400)

(kill woody-beep)
(show-graphviz-synth ding)

(ding)
(tick)

(kill fallout-wind)

;;;;;;;;;;;
;;Samples;;
;;;;;;;;;;;
(do (println art/samples) (voice/samples))

(def clap-s (freesound-sample 48310))
(def clap2-s (freesound-sample 132676))

(def waves-s (freesound-sample 163120))
(def waves (waves-s :rate 0.8 :vol 0.5 :loop? 1))
(def birds-s (freesound-sample 184870))
(def birds (birds-s :rate 0.2 :loop? 1))
(def bubbles-s (freesound-sample 104950))
(def bubbles (bubbles-s))
(def wind ((freesound-sample 81188)))

(def monkeys ((freesound-sample 93993)))

(ctl birds :rate -1)
(ctl waves :rate -1)

(kill waves)
(kill birds)
(stop)

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Busses - Wiring synths ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;
(do (println art/busses) (voice/busses))

;;Audio busses are just ids preallocated at boot.
(defonce tri-bus (audio-bus "triangle bus"))
(defonce sin-bus (audio-bus "sin bus"))

(println sin-bus)

;;Triangle wave
(defsynth tri-synth [out-bus 0 freq 5] (out:kr out-bus (lf-tri:kr freq)))
;;Sin wave
(defsynth sin-synth [out-bus 0 freq 5] (out:kr out-bus (sin-osc:kr freq)))

(defonce main-g  (group "main"))
(defonce early-g (group "early" :head main-g))
(defonce late-g  (group "late"  :after early-g))

(def tri-synth-inst (tri-synth [:tail early-g] :out-bus tri-bus))
(def sin-synth-inst (sin-synth [:tail early-g] :out-bus sin-bus))

(defsynth modulated-freq-tri [freq-bus 0 mid-freq 220 freq-amp 55]
  (let [freq (+ mid-freq (* (in:kr freq-bus) freq-amp))]
    (out 0 (pan2 (lf-tri freq)))))

(def mft (modulated-freq-tri [:tail late-g] :freq-bus sin-bus))

(ctl mft :freq-bus tri-bus)
(ctl tri-synth-inst :freq 0.2)

(kill mft)

;;;;;;;;;;;;;;;
;;Instruments;;
;;;;;;;;;;;;;;;
(do (println art/instrument) (voice/instruments))

;;Sampled
(comment (require '[overtone.inst.sampled-piano :as s-piano]))
(require '[overtone.inst.piano :as piano])
(require '[overtone.inst.synth :as o-synth])

(piano/piano :note 50)
(s-piano/sampled-piano :note 50)
(o-synth/cs80lead)
(o-synth/supersaw)

(stop)

(def piece [:E4 :F#4 :B4 :C#5 :D5 :F#4 :E4 :C#5 :B4 :F#4 :D5 :C#5])
(doseq [n piece] (s-piano/sampled-piano :note (note n)) (Thread/sleep 200))

;;;;;;;;;;
;;Timing;;
;;;;;;;;;;
(do (voice/timing) (println art/time))

(defonce timing-g (group "codemash timing" :tgt (foundation-safe-pre-default-group)))

;;The wires
(defonce root-trigger-bus (control-bus))
(defonce root-count-bus   (control-bus))
(defonce beat-trigger-bus (control-bus))
(defonce beat-count-bus   (control-bus))

(defonce count-trigger-id (trig-id))
(def current-beat 29)

(defsynth trigger [rate 100 out-bus 0]
  (out:kr out-bus (impulse:kr rate)))

(defsynth counter [in-bus 0 out-bus 0]
  (out:kr out-bus (pulse-count:kr (in:kr in-bus))))

(defsynth divider [div 32 in-bus 0 out-bus 0]
  (out:kr out-bus (pulse-divider (in:kr in-bus) div)))

(defonce root-trigger (trigger [:head timing-g] :rate 100 :in-bus root-trigger-bus))

(defonce root-count   (counter [:after root-trigger] :in-bus root-trigger-bus :out-bus root-count-bus))

(defonce beat-trigger (divider [:after root-trigger] :div current-beat :in-bus root-trigger-bus :out-bus beat-trigger-bus))

(defonce beat-count   (counter [:after beat-trigger] :in-bus beat-trigger-bus :out-bus beat-count-bus))

(defsynth get-beat [] (send-trig (in:kr beat-trigger-bus) count-trigger-id (+ (in:kr beat-count-bus) 1)))

(defonce beat (get-beat [:after beat-count]))

(require '[overtone.inst.drum :as drum])
(on-trigger count-trigger-id (fn [x] (drum/kick)) ::beat-watch)

(ctl beat-trigger :div 29)
(ctl root-trigger :rate 100)

(remove-event-handler ::beat-watch)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Building Sequencer with buffers;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(do (voice/buffers) (println art/buffers))

;;Samples from https://github.com/stars-my-destination/samples/tree/master/sliced-p5
;;Clone and point this def to where you cloned them
(def sample-root "~/Workspace/music/samples/sliced-p5/")
(def beats-g (group "beats"))

(def kick-s (load-sample (str sample-root "kick.aif")))
(def beatbox-kick-s (freesound-sample 70631))

(defonce kick-sequencer-buffer (buffer 8))

(buffer-get kick-sequencer-buffer 0)

(sample-player kick-s)

(defsynth mono-sequencer
  [buf 0 rate 1 out-bus 0 beat-num 0 sequencer 0 numsteps 8 amp 0.7]
  (let [cnt      (in:kr beat-count-bus)
        beat-trg (in:kr beat-trigger-bus)
        bar-trg  (and (buf-rd:kr 1 sequencer cnt)
                      (= beat-num (mod cnt numsteps))
                      beat-trg)
        vol      (set-reset-ff bar-trg)]
    (out
     out-bus (* vol
                amp
                (scaled-play-buf 1 buf rate bar-trg)))))

(def kicks
  (doall
   (for [x (range 8)]
     (mono-sequencer [:tail beats-g] :buf beatbox-kick-s :beat-num x :vol 0.8
                     :sequencer kick-sequencer-buffer))))

(buffer-write! kick-sequencer-buffer [1 0 0 0 0 1 0 0])

(defonce tom-sequencer-buffer (buffer 8))
(defonce tom-s (load-sample (str sample-root "tom.aif")))

(def toms
  (doall
   (for [x (range 8)]
     (mono-sequencer [:tail beats-g]
                     :amp 0.5
                     :buf tom-s
                     :beat-num x
                     :sequencer tom-sequencer-buffer))))

(defonce shake-sequencer-buffer (buffer 8))
(defonce shake-s (load-sample (str sample-root "shaker.aif")))

(def shakes
  (doall (for [x (range 8)]
           (mono-sequencer [:tail beats-g] :buf shake-s :beat-num x
                           :sequencer shake-sequencer-buffer))))

(defonce shake2-s (load-sample (str sample-root "double-shake.aif")))
(defonce shake2-sequencer-buffer (buffer 8))

(def shakes2
  (doall
   (for [x (range 8)] (mono-sequencer [:tail beats-g] :buf shake2-s :beat-num x
                                      :sequencer shake2-sequencer-buffer))))

(defonce shake2d-s (load-sample (str sample-root "double-shake-deep.aif")))
(defonce shake2d-sequencer-buffer (buffer 8))

(def shakes2d
  (doall
   (for [x (range 8)] (mono-sequencer [:tail beats-g] :buf shake2d-s :beat-num x
                     :sequencer shake2d-sequencer-buffer))))

(defonce shake1-s (load-sample (str sample-root "single-shake.aif")))
(defonce shake1-sequencer-buffer (buffer 8))

(def shakes1
  (doall
   (for [x (range 8)] (mono-sequencer [:tail beats-g] :buf shake1-s :beat-num x
                     :sequencer shake1-sequencer-buffer))))

(buffer-write! kick-sequencer-buffer    [0 0 0 0 0 0 0 0])
(buffer-write! tom-sequencer-buffer     [0 0 0 0 0 0 0 0])
(buffer-write! shake-sequencer-buffer   [0 0 0 0 0 0 0 0])
(buffer-write! shake2-sequencer-buffer  [0 0 0 0 0 0 0 0])
(buffer-write! shake1-sequencer-buffer  [0 0 0 0 0 0 0 0])
(buffer-write! shake2d-sequencer-buffer [0 0 0 0 0 0 0 0])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Playing notes with buffers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(do (println art/notes) (voice/notes-buffers))

(defonce score-b         (buffer 128))
(defonce duration-b      (buffer 128))
(defonce bass-duration-b (buffer 128))
(defonce bass-notes-b    (buffer 128))

(defsynth woody-beep [duration-bus 0 room 0.5 damp 0.5 beat-count-bus 0 offset-bus 0 amp 1 out-bus 0]
  (let [cnt    (in:kr beat-count-bus)
        offset (buf-rd:kr 1 offset-bus cnt)
        durs   (buf-rd:kr 1 duration-bus cnt)
        trig (t-duty:kr (dseq durs INFINITE))
        freq (demand:kr trig 0 (drand offset INFINITE))
        freq (midicps freq)

        env (env-gen:ar (env-asr :release 0.25 :sustain 0.8) trig)
        tri (* 0.5 (lf-tri:ar freq))
        sin (sin-osc:ar (* 1 freq))
        sin2 (sin-osc:ar (* 1.01 freq))
        wood (bpf:ar (* (white-noise:ar) (line:kr 5 0 0.02)) freq 0.02)
        src (mix [sin sin2 tri wood])
        src (free-verb src 0.33 room damp)]
    (out:ar out-bus (* amp env (pan2 src)))))

(defsynth deep-saw [freq 100 beat-count-bus 0 offset-bus 0 duration-bus 0 out-bus 0 amp 1 pan 0 room 0.5 damp 0.5]
  (let [cnt    (in:kr beat-count-bus)
        offset (buf-rd:kr 1 offset-bus cnt)
        durs   (buf-rd:kr 1 duration-bus cnt)
        trig (t-duty:kr (dseq durs INFINITE))
        freq (demand:kr trig 0 (drand offset INFINITE))
        freq (midicps freq)

        saw1 (lf-saw:ar (* 0.5 freq))
        saw2 (lf-saw:ar (* 0.25 freq))
        sin1 (sin-osc freq)
        sin2 (sin-osc (* 1.01 freq))
        src (mix [saw1 saw2 sin1 sin2])
        env (env-gen:ar (env-asr) trig)
        src (lpf:ar src)
        src (free-verb :in src :mix 0.33 :room room :damp damp)]
    (out out-bus (* amp [src src]))))

(def w  (woody-beep :duration-bus duration-b :beat-count-bus beat-count-bus :offset-bus score-b :amp 6))

;;(kill woody-beep)
;;(kill deep-saw)
(ctl w :damp 0.5)
(ctl w :room 0.5)

(def ps (deep-saw 100 :duration-bus bass-duration-b :beat-count-bus beat-count-bus :offset-bus bass-notes-b :amp 0.8))

(ctl ps :amp 1)
(ctl ps :damp 3)
(ctl ps :room 3)

(kill ps)

(def score [:F4 :F4 :F4 :F4 :F4 :F4 :F4
            :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4
            :BB4 :BB4 :BB4 :BB4 :BB4 :BB4
            :D#4 :D#4 :D#4])

(def bass-score [:F2 :F2 :G3 :G2 :G3 :BB2 :BB2 :G2 :G2])

(def duration   [1/7])

(buffer-write! bass-duration-b (take 128 (cycle [(/ 1 3.5)])))
(buffer-write! bass-notes-b
               (take 128 (cycle (map note bass-score))))
(buffer-write! bass-notes-b
               (take 128 (cycle (map #(+ -12 (note %)) score))))

(buffer-write! score-b
               (take 128 (cycle (map #(+ 0 ( note %)) score))))

(buffer-write! duration-b
               (take 128 (cycle [1/7])))

(ctl root-trigger :rate 100)
(ctl beat-trigger :div 29)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Hardware (External devices);;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(do (println art/hardware) (voice/hardware))

(midi-connected-devices)

(on-event [:midi :note-on]
          (fn [m]
            (println m)
            (buffer-write! bass-notes-b
              (map (fn [midi-note]
                     (+ -24 (:note m) (note midi-note)))
                   (take 128 (cycle score)))))
          ::phat-bass-keyboard)

(comment
  (remove-event-handler ::phat-bass-keyboard))

;;Launchpad

(use '[launchpad.core] :reload)
(boot!)

;;Mouse

(defsynth spacey [out-bus 0 amp 1]
  (out out-bus (* amp (g-verb (blip (mouse-y 24 48) (mouse-x 1 100)) 200 8))))

(spacey)
(kill spacey)

;(stop)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Go forth and make sounds ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(do (voice/the-end) (println art/end))
(stop)
