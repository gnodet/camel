(:: pragma bea:global-element-parameter parameter="$buscaListaUFResponse1" element="ns1:BuscaListaUFResponse" location="../../../ServicosEnablement/Vivonet/BuscaListaUF/WSDL/BuscaListaUF.wsdl" ::)
(:: pragma bea:global-element-return element="ns2:Ufs" location="../WSDL/Uf.wsdl" ::)

declare namespace xf = "http://tempuri.org/ServicosNegocio/Uf/XQ/xToy/";
declare namespace ns0 = "http://www.vivo.com.br/nfo/vivonet";
declare namespace ns1 = "ld:physical/BuscaListaUF_ws";
declare namespace ns2 = "http://www.vivo.com.br/MC/Catalogo";

declare function xf:xToy($buscaListaUFResponse1 as element(ns1:BuscaListaUFResponse))
    as element(ns2:Ufs) {
        let $VO_LISTAUF := $buscaListaUFResponse1/ns0:BuscaListaUF/VO_LISTAUF
        return
            <ns2:Ufs>
                {
                    for $VO_LISTAUF_ROW in $VO_LISTAUF/ns0:VO_LISTAUF_ROW
                    return
                        <ns2:uf>
                            {
                                for $IDUF in $VO_LISTAUF_ROW/IDUF
                                return
                                    <ns2:codigo>{ xs:integer( data($IDUF) ) }</ns2:codigo>
                            }
                            {
                                for $SGUF in $VO_LISTAUF_ROW/SGUF
                                return
                                    <ns2:sigla>{ data($SGUF) }</ns2:sigla>
                            }
                            {
                                for $NMUF in $VO_LISTAUF_ROW/NMUF
                                return
                                    <ns2:nome>{ data($NMUF) }</ns2:nome>
                            }
                        </ns2:uf>
                }
            </ns2:Ufs>
};

xf:xToy(/ns1:BuscaListaUFResponse)
