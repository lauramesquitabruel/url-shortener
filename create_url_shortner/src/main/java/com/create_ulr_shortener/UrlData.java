package com.create_ulr_shortener;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/*usando as dependencias do lombok no lugar de escrver
 * os setter, getters e construtores
*/
@AllArgsConstructor
@Setter
@Getter
public class UrlData {
    private String originalUrl;
    private long expirationTime;
}
