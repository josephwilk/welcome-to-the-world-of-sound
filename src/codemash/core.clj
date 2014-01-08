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
  (:require [codemash.graphs :as graph]
            [codemash.art :as art]
            [codemash.voice :as voice]))

(use 'overtone.live)
(odoc demo)

;;;;;;;;;;;;;;;;;;;;;;;;;
;;     Waves           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;
(do
  (voice/waves)
  (println art/waves))

(demo (sin-osc))
(demo (lf-saw))
(demo (pulse))
(demo (lf-pulse:ar))

(demo (square))
(demo (lf-tri))

(stop)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Synthesis                         ;;
;;                                  ;;
;;Turning waves into complex sounds.;;
;; * Additive                       ;;
;; * Subtractive                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(voice/synthesis)

;; play{d=Duty;f=d.kr(1/[1,2,4],0,Dseq([0,3,7,12,17]+24,inf));GVerb.ar(Blip.ar(f.midicps*[1,4,8],LFNoise1.kr(1/4,3,4)).sum,200,8)}

(defsynth pop [freq 300  amp 1]
  (let [sin1 (sin-osc freq)
        sin2 (sin-osc (* 1.1 freq))
        sin3 (sin-osc (* 0.9 freq))
        wood (bpf:ar (* (white-noise:ar) (line:kr 5 0 0.02)) freq 0.02)
        src (mix [sin1 sin2 sin3 wood])

;;        env (env-gen (perc :release 0.8 ))


        ]
    (out 0 (* src))))

(show-graphviz-synth pop)

(def p (pop))

(ctl p :freq 200 :amp 0.1)
(ctl p :freq 250)
(kill p)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;UGENS - Unit generators (Sweet shop);;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; * Cheat sheet:
;;     - https://github.com/overtone/overtone/raw/master/docs/cheatsheet/overtone-cheat-sheet.pdf

(do
  (voice/ugens)
  (println art/sweet-shop))

(defsynth ding [freq 880 dur 0.2 level 0.25 pan 0.0 out-bus 0]
  (let [amp  (env-gen:ar (env-perc) :action FREE :time-scale dur)
        snd (* (sin-osc:ar freq ) amp level)]
    (out out-bus (pan2:ar snd pan))))

(defsynth tick [freq 880 dur 0.1 level 0.25 pan 0.0 out-bus 0]
  (let [amp (env-gen (env-perc) :action FREE :time-scale dur)
        snd (lpf:ar (white-noise:ar) freq)]
    (out out-bus (pan2:ar (* snd amp level) pan))))

(defsynth woody-beep [freq 300 out-bus 0 dur 0.4]
  (let [tri (* 0.5 (lf-tri:ar freq))
        sin (sin-osc:ar (* 1 freq))
        sin2 (sin-osc:ar (* 1.01 freq))
        wood (bpf:ar (* (white-noise:ar) (line:kr 5 0 0.02)) freq 0.02)
        src (mix [sin sin2 tri wood])
        src (free-verb src)
        env (env-gen:ar (env-perc :release 0.25) :action FREE :time-scale dur)]
    (out out-bus (pan2 (* env src)))))

(woody-beep :freq 400)

(kill woody-beep)
(show-graphviz-synth ding)

(ding)
(tick)

;;Samples

;;(def subby-s (freesound-sample))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Busses - Wiring synths ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(do
  (println art/busses)
  (voice/busses))

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

(comment
  (def mft (modulated-freq-tri [:tail late-g] sin-bus)))

(comment
  (ctl mft :freq-bus tri-bus))

(comment
  (ctl tri-synth-inst :freq 0.5))

(comment
  (do
    (kill mft))
  )

;;;;;;;;;;;;;;;
;;Instruments;;
;;;;;;;;;;;;;;;

(do
  (voice/instruments))

;;Sampled
(comment (require 'overtone.inst.sampled-piano))
(require '[overtone.inst.piano :as piano])

(require '[overtone.inst.sampled-piano :as s-piano])

(piano/piano :note 50)
(s-piano/sampled-piano :note 50)

;;;;;;;;;;
;;Timing;;
;;;;;;;;;;

(do
  (voice/timing)
  (println art/time))

(defonce timing-g (group "codemash timing" :tgt (foundation-safe-pre-default-group)))

;;The wires
(defonce root-trigger-bus (control-bus))
(defonce root-count-bus   (control-bus))
(defonce beat-trigger-bus (control-bus))
(defonce beat-count-bus   (control-bus))

(defonce count-trigger-id (trig-id))
(def current-beat 30)

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

(ctl beat-trigger :div 30)
(ctl root-trigger :rate 100)

(comment (remove-event-handler ::beat-watch))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Building Sequencer with buffers;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(do
  (voice/buffers)
  (println art/buffers))

(def beats-g (group "beats"))

(def kick-s (load-sample "~/Workspace/music/samples/sliced-p5/kick.aif"))

(defonce kick-sequencer-buffer (buffer 8))
(buffer-get kick-sequencer-buffer 7)

(sample-player kick-s)

(defsynth mono-sequencer
  [buf 0 rate 1 out-bus 0 beat-num 0 sequencer 0 numsteps 8 amp 1]
  (let [cnt      (in:kr beat-count-bus)
        beat-trg (in:kr beat-trigger-bus)
        bar-trg  (and (buf-rd:kr 1 sequencer cnt)
                      (= beat-num (mod cnt numsteps))
                      beat-trg)
        vol      (set-reset-ff bar-trg)]
    (out
     out-bus (* vol
                amp
                (pan2
                 (scaled-play-buf 1 buf rate bar-trg))))))


(def kicks
  (doall
   (for [x (range 8)]
     (mono-sequencer [:tail beats-g] :buf kick-s :beat-num x
                     :sequencer kick-sequencer-buffer))))

(buffer-write! kick-sequencer-buffer [1 0 0 0 0 1 0 0])

(defonce tom-sequencer-buffer (buffer 8))
(defonce tom-s (load-sample "~/Workspace/music/samples/sliced-p5/tom.aif"))

(def toms
  (doall
   (for [x (range 8)]
     (mono-sequencer [:tail beats-g] :buf tom-s :beat-num x
                     :sequencer tom-sequencer-buffer))))

(buffer-write! tom-sequencer-buffer [0 0 1 0 0 0 1 0])

(defonce shake-sequencer-buffer (buffer 8))
(defonce shake-s (load-sample "~/Workspace/music/samples/sliced-p5/shaker.aif"))

(def shakes
  (doall (for [x (range 8)]
           (mono-sequencer [:tail beats-g] :buf shake-s :beat-num x
                           :sequencer shake-sequencer-buffer))))

(buffer-write! shake-sequencer-buffer [0 1 0 0 0 0 0 0])

(defonce shake2-s (load-sample "~/Workspace/music/samples/sliced-p5/double-shake.aif"))
(defonce shake2-sequencer-buffer (buffer 8))

(def shakes2
  (doall
   (for [x (range 8)] (mono-sequencer [:tail beats-g] :buf shake2-s :beat-num x
                                      :sequencer shake2-sequencer-buffer))))

(buffer-write! shake2-sequencer-buffer [0 0 0 1 0 0 0 0])

(defonce shake2d-s (load-sample "~/Workspace/music/samples/sliced-p5/double-shake-deep.aif"))
(defonce shake2d-sequencer-buffer (buffer 8))

(def shakes2d
  (doall
   (for [x (range 8)] (mono-sequencer [:tail beats-g] :buf shake2d-s :beat-num x
                     :sequencer shake2d-sequencer-buffer))))

(buffer-write! shake2d-sequencer-buffer [0 0 0 0 0 0 0 1])


(defonce shake1-s      (load-sample "~/Workspace/music/samples/sliced-p5/single-shake.aif"))

(defonce shake1-sequencer-buffer (buffer 8))

(def shakes1
  (doall
   (for [x (range 8)] (mono-sequencer [:tail beats-g] :buf shake1-s :beat-num x
                     :sequencer shake1-sequencer-buffer))))

(buffer-write! shake1-sequencer-buffer [0 0 0 0 1 0 0 0])


;;;;;;;;;;;;;;;;;;;;;;;
;; Timing AND Buffers;;
;;;;;;;;;;;;;;;;;;;;;;;

(defonce score-b         (buffer 128))
(defonce duration-b      (buffer 128))
(defonce bass-duration-b (buffer 128))
(defonce bass-notes-b    (buffer 128))

(def score [:F4 :F4 :F4 :F4 :F4 :F4 :F4
            :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4 :G4
            :BB4 :BB4 :BB4 :BB4 :BB4 :BB4
            :D#4 :D#4 :D#4])

(def duration     [1/7])

(def score (concat
            (map #(+ -5  (note %)) score)
            (map #(+ -5  (note %)) score)
            (map #(+ -10 (note %)) score)
            (map #(+ -5  (note %)) score)
            (map #(+ -1  (note %)) score)))

(defsynth woody-beep [duration-bus 0 beat-count-bus 0 offset-bus 0 amp 1 out-bus 0]
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
        src (free-verb src)]
    (out:ar out-bus (* amp env (pan2 src)))))

    (def w  (woody-beep :duration-bus duration-b :beat-count-bus beat-count-bus :offset-bus score-b :amp 4))

(defsynth deep-saw [freq 100 beat-count-bus 0 offset-bus 0 duration-bus 0 out-bus 0 amp 1 pan 0]
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
        src (free-verb src 0.33 1 1)]
    (out out-bus (* amp [src src]))))

(def ps (deep-saw 100 :duration-bus bass-duration-b :beat-count-bus beat-count-bus :offset-bus bass-notes-b :amp 0.9))

(buffer-write! bass-duration-b (take 128 (cycle [(/ 1 3.5)])))
(buffer-write! bass-notes-b (take 128 (cycle (map note [:F2 :F2 :G3 :G2 :G3 :BB2 :BB2 :G2 :G2]))))

(buffer-write! score-b (take 128 (cycle (map note score))))
(buffer-write! duration-b (take 128 (cycle duration)))

(ctl root-trigger :rate 100)
(ctl beat-trigger :div 30)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Hardware (External devices);;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(midi-connected-devices)

(on-event [:midi :note-on]
          (fn [m]
            (println m)
            (buffer-write! bass-notes-b
              (map (fn [midi-note]
                     (+ (:note m) (note midi-note)))
                   (take 128 (cycle [:F2 :F2 :G3 :G2 :G3 :BB2 :BB2 :G2 :G2])))))
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
