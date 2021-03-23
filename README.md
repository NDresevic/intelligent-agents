# Intelligent Agents for Pickup and Delivery Problem

Labs from the course Intelligent Agents (CS-430) at EPFL Lausanne, Switzerland.

The project consists of various intelligent agents (reactive, deliberative, centralized, auction) used to solve the Pickup and Delivery Problem (PDP). 
The PDP is a constrained (multiple) [Travelling Salesman Problem](https://en.wikipedia.org/wiki/Travelling_salesman_problem) where we have a logistic company 
with a fleet of vehicles. The goals are to satisfy the customer requests (packets have to be transported from their pickup location to their delivery city) and 
optimize profit. The agents are implemented in Java using Logist Platform (a simulation environment for the PDP written on top of RePast).

[Logist Platform - documentation](https://github.com/NDresevic/intelligent-agents/blob/master/workspace/LogistPlatform.pdf)

## Rabbits Grass Simulation (Implementing the first application in RePast)

The Rabbits Grass simulation is a simulation of an ecosystem: rabbits wander around randomly on a discrete grid environment on which grass is growing randomly. 
Rabbits have an initial amount of energy, and they lose a part of it with each move. Once their energy is 0, they die. However, when an alive rabbit bumps into 
some grass, it eats the grass and gains some energy. If a rabbit gains enough energy, it reproduces. The reproduction takes some energy, so the rabbit can not 
reproduce twice within the same simulation step. The grass can be adjusted to grow at different rates and give the rabbits differing amounts of energy. It has to 
be possible to fully control the total amount of grass being grown at each simulation step. The model can be used to explore the competitive advantages of these 
variables.

This model has been described at http://ccl.northwestern.edu/netlogo/models/RabbitsGrassWeeds for the NetLogo simulation toolkit.

[REPORT - rabbits grass simulation](https://github.com/NDresevic/intelligent-agents/blob/master/rabbits/kutlesic-dresevic-in/report/322505_321682_in.pdf)
<br />[CODE - rabbits grass simulation](https://github.com/NDresevic/intelligent-agents/tree/master/rabbits/kutlesic-dresevic-in/src)

### Running the simulation

The main function accepts two arguments:
<br />`args[0]`: the parameter file to specify the slider bar's variable values. By default, we set it to "" such that you can manually modify them in the GUI.
<br />`args[1]`: whether to use batch mode to run a simulation. By default, we set it to false to use the GUI mode.

## Reactive Agent for PDP

In this exercise, a reactive agent is used to solve the Pickup and Delivery Problem. For that, we implemented a reinforcement learning algorithm (RLA) to compute 
an optimal strategy off-line. The agent then uses this strategy to travel through the network.

[PROBLEM - reactive agent](https://github.com/NDresevic/intelligent-agents/blob/master/workspace/322505-321682-ex2/doc/reactive.pdf)
<br />[REPORT - reactive agent](https://github.com/NDresevic/intelligent-agents/blob/master/workspace/322505-321682-ex2/doc/322505_321682_ex2.pdf)
<br />[CODE - reactive agent](https://github.com/NDresevic/intelligent-agents/tree/master/workspace/322505-321682-ex2/src/agents)

## Deliberative Agent for PDP

In this exercise, we use a deliberative agent to solve the Pickup and Delivery Problem. A deliberative agent does not simply react to percepts coming from the 
environment. It can build a plan that specifies the sequence of actions to be taken to reach a certain goal. A deliberative agent has goals (e.g., to deliver all 
tasks) and is fully aware of the world it is acting in. Unlike the reactive agent, the deliberative agent knows the list of tasks that must be delivered. Therefore, 
the deliberative agent can construct a plan (a certain path through the network) that guarantees the optimal delivery of tasks.

[PROBLEM - deliberative agent](https://github.com/NDresevic/intelligent-agents/blob/master/workspace/322505-321682-ex3/doc/deliberative.pdf)
<br />[REPORT - deliberative agent](https://github.com/NDresevic/intelligent-agents/blob/master/workspace/322505-321682-ex3/doc/322505_321682_ex3.pdf)
<br />[CODE - deliberative agent](https://github.com/NDresevic/intelligent-agents/tree/master/workspace/322505-321682-ex3/src/deliberative)

## Centralized Agent for PDP

Deliberative agents are very efficient in executing a plan, as long as they are not disturbed by events that have not been taken into account in the plan. 
The presence of a second deliberative agent can make the whole company very inefficient. The reason for this inefficiency is the lack of coordination between 
deliberative agents. In the case of multi-agent systems, the agents' ability to coordinate their actions in achieving a common goal is a vital condition for the 
system's overall performance. In this exercise, we coordinate the actions of a number of agents in solving the PDP problem. The simplest form of coordination is 
centralized coordination, in which one entity (e.g., the logistics company in our case) instructs the agents how to act. In our problem, centralized coordination 
means that the company builds a plan to deliver all the packages with the available vehicles and then communicates the respective parts of the plan to each vehicle. The vehicles execute the plans that were given to them.

[PROBLEM - centralized agent](https://github.com/NDresevic/intelligent-agents/blob/master/workspace/322505-321682-ex4/doc/centralized.pdf)
<br />[REPORT - centralized agent](https://github.com/NDresevic/intelligent-agents/blob/master/workspace/322505-321682-ex4/doc/322505_321682_ex4.pdf)
<br />[CODE - centralized agent](https://github.com/NDresevic/intelligent-agents/tree/master/workspace/322505-321682-ex4/src)

## Auctioning Agent for PDP

In this exercise, we design agents that compete against each other within a package delivery problem framework. The goal of each agent is to maximize its profit, 
i.e., maximize the difference between the reward for delivering all tasks and the transportation costs. We use a closed-bid first-price reverse auction to 
allocate the tasks to the agents. We assume that there is a trusted auction house that is responsible for auctioning the set of tasks. At the beginning of the 
simulation, an auction house auctions the tasks one after another to the agents. For each task, the following steps are taken: 
- The auction house publishes the details of the task, i.e., the pick-up city, the delivery city, and the task's weight are revealed to the agents. 
- Every interested agent may then submit bids for the task. The bids are integer numbers representing the payment that the agent requests for the delivery of the task. 
- The task is assigned to the agent with the lowest bid and that agent is paid according to his bid.

[PROBLEM - auction agent](https://github.com/NDresevic/intelligent-agents/blob/master/workspace/322505-321682-ex5/doc/auction.pdf)
<br />[REPORT - auction agent](https://github.com/NDresevic/intelligent-agents/blob/master/workspace/322505-321682-ex5/doc/322505_321682_ex5.pdf)
<br />[CODE - auction agent](https://github.com/NDresevic/intelligent-agents/tree/master/workspace/322505-321682-ex5/src)

### Running a tournament

Put the jar files of agents in a folder, e.g., ./agents.

Create a tournament: `java -jar './logist/logist.jar' -new 'tour' './agents'`

Run the tournament: `java -jar './logist/logist.jar' -run 'tour' './config/auction.xml'`

Save the results: `java -jar './logist/logist.jar' -score 'tour'`

## Authors

- Nevena Drešević: nevena.dresevic@epfl.ch ([NDresevic](https://github.com/NDresevic))
- Dubravka Kutlešić: dubravka.kutlesic@epfl.ch ([dkutlesic](https://github.com/dkutlesic))
