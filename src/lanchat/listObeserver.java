/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.util.List;

/**
 *
 * @author Mear
 * @param <E>
 */
public interface listObeserver<E> {
    void removeAll(List<E> fromList, List<E> removeList);
}
