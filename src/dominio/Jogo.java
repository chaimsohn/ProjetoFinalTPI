package dominio;

public record Jogo(Integer rodada,
                   DataDoJogo data,
                   Time mandante,
                   Time visitante,
                   Time vencedor,
                   String arena,
                   Integer mandantePlacar,
                   Integer visitantePlacar,
                   String estadoMandante,
                   String estadoVisitante,
                   String estadoVencedor){
    public Resultado getResultado(){
        return new Resultado(this.mandantePlacar, this.visitantePlacar);
    }

    public Integer getTodosGols() {
        Integer todosOsGosl = this.mandantePlacar + this.visitantePlacar;
        return  todosOsGosl;
    }

}
