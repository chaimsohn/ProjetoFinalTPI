package impl;

import dominio.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CampeonatoBrasileiroImpl {

    private Map<Integer, List<Jogo>> brasileirao;
    private List<Jogo> jogos;
    private Predicate<Jogo> filtro;

    public CampeonatoBrasileiroImpl(Path arquivo, Predicate<Jogo> filtro) throws IOException {
        this.jogos = lerArquivo(arquivo);
        this.filtro = filtro;
        this.brasileirao = jogos.stream()
                .filter(filtro) //filtrar por ano
                .collect(Collectors.groupingBy(
                        Jogo::rodada,
                        Collectors.mapping(Function.identity(), Collectors.toList())));

    }

    public List<Jogo> lerArquivo(Path file) throws IOException {
        List<Jogo> listaTodosJogos = new ArrayList<>();

        try {
            Stream<String> stramFile = Files.lines(file);
            List<String> arquivo = stramFile.toList();
            DateTimeFormatter formatarData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter formatarHorario = DateTimeFormatter.ofPattern("HH'h'mm");
            DateTimeFormatter formatarHorarioHh = DateTimeFormatter.ofPattern("HH':'mm");


            for (int i = 1; i< arquivo.size(); i++) {
                String linhas = arquivo.get(i);
                String[] dados = linhas.split(";");

                Integer rodada = Integer.parseInt(dados[0]);

                DataDoJogo data;
                switch (dados[2]) {
                    case ("h") -> data = new DataDoJogo(LocalDate.parse(dados[1], formatarHorario), LocalTime.parse(dados[2], formatarHorario), getDayOfWeek(dados[3]));
                    case (":") -> data = new DataDoJogo(LocalDate.parse(dados[1], formatarHorarioHh), LocalTime.parse(dados[2], formatarHorarioHh), getDayOfWeek(dados[3]));
                    default -> data = new DataDoJogo(LocalDate.parse(dados[1], formatarData), null, getDayOfWeek(dados[3]));
                };
                    Time mandante = new Time(dados[4]);
                    Time visitante = new Time(dados[5]);
                    Time vencedor = new Time(dados[6]);
                    String estadio = dados[7];
                    Integer placarMandante = Integer.valueOf(dados[8]);
                    Integer placarVisitante = Integer.valueOf(dados[9]);
                    String localMandante = dados[10];
                    String localVisitante = dados[11];
                    String localVencedor = dados[12];
                    Jogo partida = new Jogo(rodada,
                            data,
                            mandante,
                            visitante,
                            vencedor,
                            estadio,
                            placarMandante,
                            placarVisitante,
                            localMandante,
                            localVisitante,
                            localVencedor);
                    listaTodosJogos.add(partida);
                }
            }catch (IOException e){
            System.out.println("Erro ao ler arquivo" + e);
        }
        return listaTodosJogos;
        }



    public IntSummaryStatistics getEstatisticasPorJogo() {

        return todosOsJogos().stream().mapToInt(Jogo::getTodosGols).summaryStatistics();
    }

    public Map<Jogo, Integer> getMediaGolsPorJogo() {
        var mediaGols = todosOsJogos().stream().mapToDouble(Jogo::getTodosGols).summaryStatistics().getAverage();
        return null;
    }


    public List<Jogo> todosOsJogos() {

        return jogos.stream().filter(filtro).toList();
    }

    public Long getTotalVitoriasEmCasa() {
        return todosOsJogos().stream().filter(rodada -> rodada.visitantePlacar() < rodada.mandantePlacar()).count();
    }

    public Long getTotalVitoriasForaDeCasa() {
        return todosOsJogos().stream().filter(rodada -> rodada.visitantePlacar() > rodada.mandantePlacar()).count();
    }

    public Long getTotalEmpates() {
        return todosOsJogos().stream().filter(rodada -> rodada.visitantePlacar().equals(rodada.mandantePlacar())).count();
    }

    public Long getTotalJogosComMenosDe3Gols() {
        return todosOsJogos().stream().filter(rodada -> (rodada.mandantePlacar() + rodada.visitantePlacar()) < 3).count();
    }

    public Long getTotalJogosCom3OuMaisGols() {
        return todosOsJogos().stream().filter(rodada -> (rodada.mandantePlacar() + rodada.visitantePlacar()) >= 3).count();
    }

    public Map<Resultado, Long> getTodosOsPlacares() {
        return todosOsJogos().stream().collect(Collectors.groupingBy(Jogo::getResultado, Collectors.counting()));
    }

    public Map.Entry<Resultado, Long> getPlacarMaisRepetido() {
        return getTodosOsPlacares().entrySet().stream().min(Map.Entry.comparingByValue()).get();
    }

    public Map.Entry<Resultado, Long> getPlacarMenosRepetido() {
        return getTodosOsPlacares().entrySet().stream().max(Map.Entry.comparingByValue()).get();
    }

    private List<Time> getTodosOsTimes() {

        ArrayList<Time> times = new ArrayList<>();
        for(Jogo partida : todosOsJogos()){
            times.add(partida.mandante());
        }
        times.stream().distinct();
        return times.stream().toList();
    }

    private List<Jogo> getTodosOsJogosPorTimeComoMandantes(Time time) {
        return todosOsJogos().stream().filter(rodada -> rodada.mandante().nome().equals(time.nome())).toList();
    }

    private List<Jogo> getTodosOsJogosPorTimeComoVisitante(Time time) {
        return todosOsJogos().stream().filter(rodada -> rodada.visitante().nome().equals(time.nome())).toList();
    }

    public List<PosicaoTabela> getTabela() {
        Set<PosicaoTabela> posicoes = new HashSet<>();

        for (Time time : getTodosOsTimes()) {
            posicoes.add(new PosicaoTabela(
                    new Time(time.nome()),
                    getPontosPorTime(time),
                    getVitoriasTime(time),
                    getDerrotasTime(time),
                    getEmpatesPorTime(time),
                    getGolsFeitosPorTime(time),
                    getGolsSofridosPorTime(time),
                    saldoDeGols(time))
            );
        }

        Comparator<PosicaoTabela> comparator = Comparator.comparing(PosicaoTabela::pontos).thenComparing(PosicaoTabela::vitorias).thenComparing(PosicaoTabela::saldoDeGols).reversed();

        return posicoes.stream().sorted(comparator).toList();
    }

    private Integer saldoDeGols(Time time) {
        Integer saldoGols = 0;
        saldoGols = getGolsFeitosPorTime(time) - getGolsSofridosPorTime(time);
        return saldoGols;
    }

    private Integer getPontosPorTime(Time time) {

        Integer pontos = (3 * getVitoriasTime(time) + getEmpatesPorTime(time));
        return pontos;
    }


    private DayOfWeek getDayOfWeek(String dia) {
        return switch (dia){
            case "Segunda-feira" -> DayOfWeek.MONDAY;
            case "Terça-feira" -> DayOfWeek.TUESDAY;
            case "Quarta-feira" -> DayOfWeek.WEDNESDAY;
            case "Quinta-feira" -> DayOfWeek.THURSDAY;
            case "Sexta-feira" -> DayOfWeek.FRIDAY;
            case "Sábado" -> DayOfWeek.SATURDAY;
            case "Domingo" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    public Map<Integer, Integer> getTotalGolsPorRodada() {
        return todosOsJogos().stream().collect(Collectors.toMap(Jogo::rodada, jogo -> jogo.getTodosGols()));
    }

    private Integer getVitoriasTime(Time time){

        Integer totalVitorias = 0;
        Integer vitoriasVisitante = Math.toIntExact(getTodosOsJogosPorTimeComoVisitante(time).stream().filter(rodada -> rodada.visitantePlacar() > rodada.mandantePlacar()).count());
        Integer vitoriasMandante = Math.toIntExact(getTodosOsJogosPorTimeComoMandantes(time).stream().filter(rodada -> rodada.mandantePlacar() > rodada.visitantePlacar()).count());

        totalVitorias = vitoriasMandante + vitoriasVisitante;
        return totalVitorias;
    }

    private Integer getEmpatesPorTime(Time time){
        Integer empatesTime = Math.toIntExact(getTodosOsJogosPorTimeComoVisitante(time).stream().filter(rodada -> rodada.visitantePlacar().equals(rodada.mandantePlacar())).count());

        return empatesTime;

    }

    private Integer getDerrotasTime(Time time){
        Integer totalDerrotas = 0;
        Integer derrotasVisitante = Math.toIntExact(getTodosOsJogosPorTimeComoVisitante(time).stream().filter(rodada -> rodada.visitantePlacar() < rodada.mandantePlacar()).count());
        Integer derrotasMandante = Math.toIntExact(getTodosOsJogosPorTimeComoMandantes(time).stream().filter(rodada -> rodada.mandantePlacar() < rodada.visitantePlacar()).count());

        totalDerrotas = derrotasMandante + derrotasVisitante;
        return totalDerrotas;
    }

    private Integer getGolsFeitosPorTime(Time time){

        Integer totalGolsFeitos = 0;
        totalGolsFeitos = getTodosOsJogosPorTimeComoVisitante(time).stream().map(jogo -> jogo.visitantePlacar()).reduce(Math.toIntExact(totalGolsFeitos), Integer::sum);
        totalGolsFeitos = getTodosOsJogosPorTimeComoMandantes(time).stream().map(jogo -> jogo.mandantePlacar()).reduce(Math.toIntExact(totalGolsFeitos), Integer::sum);
        return totalGolsFeitos;
    }

    private Integer getGolsSofridosPorTime(Time time){

        Integer totalGolsSofridos = 0;
        totalGolsSofridos = getTodosOsJogosPorTimeComoVisitante(time).stream().map(jogo -> jogo.mandantePlacar()).reduce(Math.toIntExact(totalGolsSofridos), Integer::sum);
        totalGolsSofridos = getTodosOsJogosPorTimeComoMandantes(time).stream().map(jogo -> jogo.visitantePlacar()).reduce(Math.toIntExact(totalGolsSofridos), Integer::sum);
        return totalGolsSofridos;
    }




}