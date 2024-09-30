package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver
        extends SequentialSolver
{

    //static ConcurrentSkipListSet<Integer> visited = new ConcurrentSkipListSet<>();
    //static ConcurrentHashMap<Integer, Integer> predecessor = new ConcurrentHashMap<>();
    static boolean found = false;
    static int forkAfter;
    private int begin;

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
        begin = start;
        visited = new ConcurrentSkipListSet<>();
        predecessor = new ConcurrentHashMap<>();
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */
    public ForkJoinSolver(Maze maze, Set<Integer> visit, Map<Integer, Integer> pred, int begin)
    {
        super(maze);
        visited = visit;
        predecessor = pred;
        this.start = begin;
    }

    public ForkJoinSolver(Maze maze, int forkafter){
        super(maze);
        forkAfter = forkafter;
        visited = new ConcurrentSkipListSet<>();
    }
    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */
    @Override
    public List<Integer> compute()
    {
        return parallelSearch();
    }

    private List<Integer> parallelSearch()
    {
        List<ForkJoinSolver> nrOfChilds = new ArrayList<>();
        Stack<Integer> grannar = new Stack<>();
        int steps = 0;

        int player = maze.newPlayer(start);
        int currNode = start;
        grannar.push(currNode);




        while(!grannar.isEmpty())
        {
            if (found) return null;

            currNode = grannar.pop();

            if(maze.hasGoal(currNode))
            {   maze.move(player, currNode);
                return fullPath(pathFromTo(start, currNode), currNode);
            }

            if(visited.add(currNode))
            {
                maze.move(player, currNode);
                steps++;

                for(int nb : maze.neighbors(currNode)) {
                    if (!visited.contains(nb))
                    {
                        predecessor.put(nb, currNode);
                        if (steps % forkAfter == 0 && amountAvailNeigh(maze.neighbors(currNode)) > 1)
                        {
                            ForkJoinSolver child = new ForkJoinSolver(maze, visited, predecessor, nb);
                            nrOfChilds.add(child);
                            child.fork();

                        } else {
                            grannar.push(nb);

                        }
                    }
                }
            }
        }
        for(ForkJoinSolver child : nrOfChilds)
        {
            List<Integer> result = child.join();
            if (result != null)
            {
                return result;
            }
        }

        return null;
    }
    private int amountAvailNeigh(Set<Integer> neigh){
        int amount = 0;
        for (int neighbor : neigh){
            if(!visited.contains(neighbor)) amount++;
        }
        return amount;
    }

    private List<Integer> fullPath(List<Integer> childPath, int startNode){
        List<Integer> path = new ArrayList<>();
        int currNode = startNode;
        while(currNode != maze.start())
        {
            path.add(currNode);
            currNode = predecessor.get(currNode);
        }
        path.add(maze.start());
        Collections.reverse(path);
        return path;

    }
}