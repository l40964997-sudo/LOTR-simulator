# Report
Jiayi Yang() & Finn Chen (s) & Tiancheng Yang ()

## Introduction

> *Very briefly describe what your program does.*
 
>Expected length: ~100 words
> This program is a Lord of the Rings themed turn-based RTS simulator. 
> The user designs a map of locations and routes on a graph editor,
> then populates it with armies drawn from seven factions split across two teams (Free Peoples: Men, Elves, Dwarves, Hobbits;
> Sauron's Servants: Mordor, Isengard). Each step, every army rolls a random action (Fortify, Rally, Skirmish, ...), advances along a smart-pathing route,
> may trigger one of nine map events, and resolves any battles in three phases (volley, charge, melee) with morale and rout checks. 
> The user can take direct control of one army, build custom rosters, undo any edit, save/load to JSON, and hear procedural sound effects plus a looping music playlist.



## Program design

> *Here you go over the structure of the program. Try not to go too in-depth here implementation-wise, but rather discuss the important components and relations between them. 
> If you think it can help, feel free to add a simple diagram here. The design of the program should be clear to the reader. 
> 
> In particular, describe the model of the program. How is it structured? How did you make sure to separate the different aspects of the program?
> How do the `model`, `view` and `controller` interact with each other?
> Additionally, you should include some design decisions in here. There is no need to provide an explanation for every single thing, 
> but there are often multiple ways of implementing a feature and in those cases it makes sense to state why you chose one over the other.*

> Expected length: as much as you need to explain the above.


## Evaluation of the program

> *Discuss the stability of your implementation. What works well? Are there any bugs? Is everything tested properly? Are there still features that have not been implemented? Also, if you had the time, what improvements would you make to your implementation? Are there things which you would have done completely differently?*

>Expected length: ~300-500 words
> 

## Questions

Please answer the following questions:

1. In this assignment, the program should follow the Model View Controller (MVC) pattern. Please explain the design of the program in terms of the MVC pattern. Specifically try to answer the following questions:
   - MVC consists of three components: Model, view and controller. Can you please explain the role of each component? Please provide examples of these roles from the assignment. How are these three roles (i.e. Model, view and controller) are implemented in the assignment?
   - MVC enforces special constraints on the dependencies between its three components: Model, view and controller. Please explain these constraints, and why are they important?

___

Answer:

___

2. The Swing library provides the ability to create nested user interface components. In this assignment, you created multiple JPanel components on the user interface. These contain other user interface components to build-up a tree of user interface components.
Which design pattern does Swing implement to create a hierarchy of user interface components? Please explain this pattern and how it is implemented in Swing.

___

Answer:

___

3. The Observer pattern is useful to implement the MVC pattern. Can you please explain the relationship between the Observer pattern and the MVC pattern?
Please provide an example from the assignment on how the Observer pattern supports implementing the MVC pattern.

___

Answer:The Observer pattern is the engine drives the communication between the Model and the View. 

___

## Process evaluation

> *Describe shortly the process that led to the final code and the report. What was easy, what was difficult? Did you make interesting mistakes? What have you learned from this assignment?*

> Expected length: ~150 words

## Conclusions

> *Add a very short summary/concluding remarks here*
