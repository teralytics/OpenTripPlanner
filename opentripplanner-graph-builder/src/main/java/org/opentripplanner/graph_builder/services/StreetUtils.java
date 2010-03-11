/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.services;

import java.util.Collection;

import org.opentripplanner.routing.core.DeadEnd;
import org.opentripplanner.routing.core.GenericStreetIntersectionVertex;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Intersection;
import org.opentripplanner.routing.core.IntersectionVertex;
import org.opentripplanner.routing.core.Vertex;

public class StreetUtils {


    public static void unify(Graph graph, Collection<Intersection> intersections) {
        for (Intersection intersection: intersections) {
            if (intersection.getDegree() == 1) {
                //a dead end can be represented more simply as a special case.
                IntersectionVertex v = (IntersectionVertex) intersection.vertices.get(0);
                DeadEnd d = new DeadEnd(v);
                graph.addVertex(d);
                graph.removeVertex(v);
            } else if (intersection.getDegree() == 2) {
                //this is an "intersection" of only two streets, and so can be simplified down
                //to a single vertex.
                IntersectionVertex v1 = (IntersectionVertex) intersection.vertices.get(0);
                GenericVertex generic = new GenericStreetIntersectionVertex(v1.getLabel(), v1.getX(), v1.getY(), v1.getName());
                for (Vertex v : intersection.vertices) {
                    IntersectionVertex iv = (IntersectionVertex) v;
                    if (iv.inStreet != null) {
                        generic.addIncoming(iv.inStreet);
                        iv.inStreet.setToVertex(generic);
                    }
                    if (iv.outStreet != null) {
                        generic.addOutgoing(iv.outStreet);
                        iv.outStreet.setFromVertex(generic);
                    }
                    graph.removeVertex(v);
                }
                graph.addVertex(generic);
            }
        }
    }

}
