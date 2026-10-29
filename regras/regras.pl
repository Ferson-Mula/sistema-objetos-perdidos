% ============================================================
% REGRAS DE NEGÓCIO DO SISTEMA DE ACHADOS E PERDIDOS (UJC)
%
% Este ficheiro está escrito em estilo Prolog:
%   - um FACTO diz uma verdade:            identificacao(documento).
%   - uma REGRA diz "isto é verdade se":   sensivel(C) :- identificacao(C).
%   - as palavras com MAIÚSCULA são variáveis (C, P, O, R).
%   - "\+" quer dizer "não" (não consegue provar).
%
% Para mudar uma regra basta editar este ficheiro e voltar a
% executar o programa. Não é preciso mexer no código Java.
% ============================================================


% ---------- 1. Tipos de objecto ----------

% Documentos e cartões identificam uma pessoa.
identificacao(documento).
identificacao(cartao).

% Telemóveis e carteiras guardam dados pessoais.
dados_pessoais(telemovel).
dados_pessoais(carteira).

% Objectos sensíveis nunca mostram foto nem descrição no catálogo público (RN06).
sensivel(C) :- identificacao(C).
sensivel(C) :- dados_pessoais(C).

% A foto é obrigatória, excepto para documentos e cartões (RN02).
foto_obrigatoria(C) :- \+ identificacao(C).

% Prazo de guarda em dias (RN17).
prazo_guarda(C, 30) :- identificacao(C).
prazo_guarda(C, 90) :- \+ identificacao(C).


% ---------- 2. Destino de objectos não reclamados (P04) ----------

destino_sugerido(C, entregar_a_entidade_emissora) :- identificacao(C).
destino_sugerido(C, destruicao_segura) :- dados_pessoais(C).
destino_sugerido(electronico, reciclagem).
destino_sugerido(C, doacao) :- \+ especial(C).

especial(C) :- identificacao(C).
especial(C) :- dados_pessoais(C).
especial(electronico).


% ---------- 3. Correspondência entre uma procura (P) e um objecto (O) ----------
% O programa Java calcula as características (mesma_cor, local_proximo, ...).
% Aqui decidimos se, com essas características, vale a pena avisar o dono (RN08).

correspondencia(P, O) :- mesma_categoria(P, O), mesma_cor(P, O), local_proximo(P, O).
correspondencia(P, O) :- mesma_categoria(P, O), mesma_marca(P, O), data_compativel(P, O).
correspondencia(P, O) :- mesma_categoria(P, O), mesma_cor(P, O), mesma_marca(P, O).
correspondencia(P, O) :- mesma_categoria(P, O), descricao_parecida(P, O), local_proximo(P, O).
correspondencia(P, O) :- mesma_categoria(P, O), descricao_parecida(P, O), mesma_cor(P, O).


% ---------- 4. Verificação de uma reclamação (R) ----------
% A reclamação só segue para o funcionário se a prova for suficiente (RN11).

verificacao_suficiente(R) :- detalhe_correcto(R).
verificacao_suficiente(R) :- detalhe_parcial(R), tem_provas(R).
verificacao_suficiente(R) :- sem_detalhe_oculto(R), resposta_longa(R), tem_provas(R).
