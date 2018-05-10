package br.tiagohm.materialfilechooser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import br.tiagohm.breadcrumbview.BreadCrumbItem;
import br.tiagohm.breadcrumbview.BreadCrumbView;
import br.tiagohm.easyadapter.EasyAdapter;
import br.tiagohm.easyadapter.EasyInjector;
import br.tiagohm.easyadapter.Injector;

//TODO Estensível para Dropbox, FTP, Drive, etc
//TODO Botão pra ver outras informações
//TODO Opção pra pré-visualizar um arquivo?
public class MaterialFileChooser {

    //Variáveis finais.
    private final Builder builder;
    private final EasyAdapter listaDeArquivosEPastasAdapter = EasyAdapter.create();
    private final Context context;
    //Views.
    private MaterialDialog dialog;
    private BreadCrumbView<File> mCaminhoDoDiretorio;
    private RecyclerView mListaDeArquivosEPastas;
    private TextView mTamanhoTotal;
    private TextView mQuantidadeDeItens;
    private ImageView mBotaoVoltar;
    private ImageView mIrParaDiretorioInicial;
    private TextView mQuantidadeDeItensSelecionados;
    private ImageView mBotaoBuscar;
    private View mCampoDeBuscaBox;
    private EditText mCampoDeBusca;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CheckBox mSelecionarTudo;
    private FloatingActionButton mBotaoCriarPasta;
    //Variáveis
    private boolean showHiddenFiles;
    private boolean allowMultipleFiles;
    private boolean allowBrowsing;
    //TODO Permitir a criação de diretório.
    private boolean allowCreateFolder;
    private boolean allowSelectFolder;
    private boolean showFoldersFirst;
    private boolean showFiles;
    private boolean showFolders;
    private boolean restoreFolder;
    private File initialFolder;
    private File pastaAtual;
    private LinkedList<File> pilhaDeCaminhos = new LinkedList<>();
    private List<File> arquivosAtuais;
    private Set<File> arquivosSelecionados = Collections.newSetFromMap(new ConcurrentHashMap<File, Boolean>());
    private long tamanhoTotalDosArquivosSelecionados = 0;
    private ChooserFileFilter chooserFileFilter = new ChooserFileFilter();
    private CheckBox arquivoAnteriormenteSelecionadoCb = null;
    private File arquivoAnteriormenteSelecionado = null;
    private OnFileChooserListener fileChooserListener;
    private String busca = null;
    private int minSelectedItems = 0;
    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //nada
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //nada
        }

        @Override
        public void afterTextChanged(Editable s) {
            busca = s.toString().toLowerCase();
            loadCurrentFolder();
        }
    };
    private List<Filter> filters = new ArrayList<>();
    private Sorter ordenacao = Sorter.SORT_BY_NAME_ASC;
    private Map<File, Boolean> selecionarTudoStatus = new ConcurrentHashMap<>();
    private PrefsManager prefsManager;

    public MaterialFileChooser(@NonNull Context context) {
        this(context, null);
    }

    public MaterialFileChooser(@NonNull Context context, String title) {
        this.context = context;
        //Builder.
        builder = new Builder(context, title);
        init(context);
    }

    public MaterialFileChooser(@NonNull Context context, @StringRes int title) {
        this.context = context;
        //Builder.
        builder = new Builder(context, title);
        init(context);
    }

    private int getIconByExtension(Context context, File file) {
        final int index = file.getName().lastIndexOf(".");
        if (index < 0) return R.drawable.arquivo;
        final String ext = file.getName().substring(index + 1).toLowerCase();
        switch (ext) {
            case "mp4":
                return R.drawable.video;
            case "c":
            case "cpp":
            case "cs":
            case "js":
            case "h":
            case "java":
            case "kt":
            case "php":
            case "xml":
                return R.drawable.codigo;
            case "avi":
                return R.drawable.avi;
            case "doc":
                return R.drawable.doc;
            case "flv":
                return R.drawable.flv;
            case "jpg":
            case "jpeg":
                return R.drawable.jpg;
            case "json":
                return R.drawable.json;
            case "mov":
                return R.drawable.mov;
            case "mp3":
                return R.drawable.mp3;
            case "pdf":
                return R.drawable.pdf;
            case "txt":
                return R.drawable.txt;
            default:
                return R.drawable.arquivo;
        }
    }

    private boolean constainsSelectedChildren(File parent) {
        for (File file : arquivosSelecionados) {
            if (file.getAbsolutePath().startsWith(parent.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    private void init(@NonNull final Context context) {
        //Preferencias.
        prefsManager = new PrefsManager(context);
        //Cores.
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.mfc_theme_foreground_color, typedValue, true);
        final int foregroundColor = typedValue.data;
        theme.resolveAttribute(R.attr.mfc_theme_background_color, typedValue, true);
        final int backgroundColor = typedValue.data;
        //Adapter.
        listaDeArquivosEPastasAdapter.register(File.class, R.layout.file_item, new EasyInjector<File>() {
            @Override
            public void onInject(final File file, Injector injector) {
                //Seta o ícone de acordo com o tipo do arquivo.
                if (FileHelper.isFolder(file)) {
                    injector.image(R.id.iconeDoArquivo, R.drawable.pasta);
                } else {
                    injector.image(R.id.iconeDoArquivo, getIconByExtension(context, file));
                }
                //Seta o ícone de arquivo protegido.
                if (FileHelper.isProtected(file)) {
                    injector.image(R.id.protecaoDoArquivo, R.drawable.cadeado);
                } else {
                    injector.image(R.id.protecaoDoArquivo, null);
                }
                //Seta o ícone de que contém arquivos selecionados.
                if (FileHelper.isFolder(file) && constainsSelectedChildren(file)) {
                    injector.image(R.id.pastaComItensSelecionados, R.drawable.asterisco);
                } else {
                    injector.image(R.id.pastaComItensSelecionados, null);
                }
                //Seta a opacidade se o arquivo é oculto.
                if (FileHelper.isHidden(file)) {
                    injector.find(R.id.iconeDoArquivo).setAlpha(0.4f);
                } else {
                    injector.find(R.id.iconeDoArquivo).setAlpha(1f);
                }
                //Seta o texto com o nome do arquivo.
                injector.text(R.id.nomeDoArquivo, file.getName());
                //Seta o tamanho do arquivo.
                if (FileHelper.isFile(file)) {
                    //Tamanho em bytes.
                    injector.text(R.id.tamanhoDoArquivo, FileHelper.sizeAsString(file, false));
                } else {
                    //Tamanho em quantidade de itens.
                    //Obtém a quantidade de itens.
                    final int itemCount = FileHelper.itemCount(file, chooserFileFilter);
                    //Formatação do texto de acordo com a pluralidade.
                    final int stringRes = itemCount > 1 ? R.string.quantidade_itens_pasta_plural : R.string.quantidade_itens_pasta_singular;
                    injector.text(R.id.tamanhoDoArquivo,
                            MaterialFileChooser.this.context.getString(stringRes, itemCount));
                }
                //Seta a data de modificação do arquivo.
                injector.text(R.id.dataDaUltimaModificacao, FileHelper.lastModified(file));
                //Permitir selecionar pastas.
                injector.visibility(R.id.botaoSelecionarArquivo,
                        !allowSelectFolder && FileHelper.isFolder(file) ? View.GONE : View.VISIBLE);
                //Evento de clique.
                //Cliquei numa pasta
                if (FileHelper.isFolder(file)) {
                    injector.onClick(0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Vá para a pasta.
                            goTo(file);
                        }
                    });
                } else {
                    //Sem evento.
                    injector.onClick(0, null);
                }
                //Selecionei/Deselecionei um arquivo.
                CheckBox checkBox = injector.find(R.id.botaoSelecionarArquivo);
                checkBox.setTag(file);
                //Marcao checkbox se este é de um arquivo selecionado.
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(arquivosSelecionados.contains(file));
                //Evento.
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        //Seleciona ou deseleciona o arquivo.
                        selecionarArquivo(buttonView, file, isChecked);
                    }
                });
                //Atualizar
                mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        loadCurrentFolder();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
                //Selecionar tudo
                mSelecionarTudo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        //Seleciona ou não os arquivo da pasta atual.
                        selecionarTudoStatus.put(pastaAtual, checked);
                        for (File file : arquivosAtuais) {
                            if (allowSelectFolder || !FileHelper.isFolder(file)) {
                                selecionarArquivo(null, file, checked);
                            }
                        }
                        //Recarrega os itens exibidos.
                        loadCurrentFolder();
                    }
                });
            }
        });
        //Valores padrão.
        initialFolder(Environment.getExternalStorageDirectory());
        showHiddenFiles(false);
        showFoldersFirst(true);
        allowCreateFolder(false);
        allowMultipleFiles(false);
        allowSelectFolder(false);
        allowBrowsing(true);
        showFiles(true);
        showFolders(true);
        restoreFolder(false);
        minSelectedItems(0);
        //Quantidade de itens inicial.
        exibirQuantidadeDeItensSelecionados();
        //Adiciona os eventos
        mCaminhoDoDiretorio.setBreadCrumbListener(new BreadCrumbView.BreadCrumbListener<File>() {
            @Override
            public void onItemClicked(BreadCrumbView<File> breadCrumbView, BreadCrumbItem<File> breadCrumbItem, int i) {
                goTo(breadCrumbItem.getSelectedItem());
            }

            @Override
            public boolean onItemValueChanged(BreadCrumbView<File> breadCrumbView, BreadCrumbItem<File> breadCrumbItem, int i, File file, File t1) {
                return false;
            }
        });
        mBotaoVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back();
            }
        });
        mIrParaDiretorioInicial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStart();
            }
        });
        mBotaoBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCampoDeBuscaBox.getVisibility() == View.VISIBLE) {
                    mCampoDeBuscaBox.setVisibility(View.GONE);
                } else {
                    mCampoDeBuscaBox.setVisibility(View.VISIBLE);
                }
            }
        });
        mCampoDeBusca.removeTextChangedListener(textWatcher);
        mCampoDeBusca.addTextChangedListener(textWatcher);
        mBotaoCriarPasta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(context)
                        .title(R.string.criar_pasta_title)
                        .titleColor(foregroundColor)
                        .inputRangeRes(1, -1, R.color.criar_pasta_input_out_range)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .negativeText(android.R.string.cancel)
                        .negativeColor(foregroundColor)
                        .positiveColor(foregroundColor)
                        .backgroundColor(backgroundColor)
                        .input(R.string.criar_pasta_edittext_hint, 0, false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                final File novaPasta = new File(pastaAtual, input.toString());
                                try {
                                    if (!novaPasta.mkdir()) {
                                        Toast.makeText(context, "error", Toast.LENGTH_SHORT).show();
                                    } else {
                                        loadCurrentFolder();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(context, "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).show();
            }
        });
    }

    private void selecionarArquivo(CompoundButton buttonView, File file, boolean selecionar) {
        //Checkbox selecionado.
        if (selecionar) {
            //Não é multi-selecionável e tem um arquivo selecionado já.
            if (!allowMultipleFiles && arquivoAnteriormenteSelecionadoCb != null) {
                CheckBox cb = arquivoAnteriormenteSelecionadoCb;
                arquivoAnteriormenteSelecionadoCb = null;
                //Dois arquivos que estão na mesma pasta.
                if (buttonView != cb &&
                        Objects.equals(file.getParent(), arquivoAnteriormenteSelecionado.getParent())) {
                    //Desmarca o que está selecionado.
                    cb.setChecked(false);
                } else {
                    //Remove o que está selecionado.
                    arquivosSelecionados.remove(arquivoAnteriormenteSelecionado);
                    arquivoAnteriormenteSelecionado = null;
                }
            }
            //Adiciona o arquivo.
            if (!arquivosSelecionados.contains(file)) {
                tamanhoTotalDosArquivosSelecionados += FileHelper.isFolder(file) ? 0 : file.length();
            }
            arquivosSelecionados.add(file);
        } else {
            //Remove o arquivo.
            if (arquivosSelecionados.contains(file)) {
                tamanhoTotalDosArquivosSelecionados -= FileHelper.isFolder(file) ? 0 : file.length();
            }
            arquivosSelecionados.remove(file);
            arquivoAnteriormenteSelecionado = null;
        }
        //Marca o arquivo que foi selecionado.
        arquivoAnteriormenteSelecionadoCb = (CheckBox) buttonView;
        arquivoAnteriormenteSelecionado = file;
        //Atualiza o número de pastas selecionadas de acordo com a pluralidade.
        exibirQuantidadeDeItensSelecionados();
    }

    @SuppressLint("StringFormatMatches")
    private void exibirQuantidadeDeItensSelecionados() {
        //Atualiza o número de pastas selecionadas de acordo com a pluralidade.
        if (arquivosSelecionados.size() > 1) {
            mQuantidadeDeItensSelecionados.setText(
                    context.getString(R.string.quantidade_itens_selecionados_plural, arquivosSelecionados.size(), FileHelper.sizeToString(tamanhoTotalDosArquivosSelecionados)));
        } else {
            mQuantidadeDeItensSelecionados.setText(
                    context.getString(R.string.quantidade_itens_selecionados_singular, arquivosSelecionados.size(), FileHelper.sizeToString(tamanhoTotalDosArquivosSelecionados)));
        }
    }

    public MaterialFileChooser onFileChooserListener(OnFileChooserListener fileChooserListener) {
        this.fileChooserListener = fileChooserListener;
        return this;
    }

    public MaterialFileChooser showFiles(boolean showFiles) {
        this.showFiles = showFiles;
        return this;
    }

    public MaterialFileChooser showFolders(boolean showFolders) {
        this.showFolders = showFolders;
        return this;
    }

    public MaterialFileChooser showHiddenFiles(boolean showHiddenFiles) {
        this.showHiddenFiles = showHiddenFiles;
        return this;
    }

    public MaterialFileChooser allowSelectFolder(boolean allowSelectFolder) {
        this.allowSelectFolder = allowSelectFolder;
        return this;
    }

    public MaterialFileChooser allowMultipleFiles(boolean allowMultipleFiles) {
        this.allowMultipleFiles = allowMultipleFiles;
        mSelecionarTudo.setVisibility(allowMultipleFiles ? View.VISIBLE : View.GONE);
        return this;
    }

    public MaterialFileChooser allowBrowsing(boolean allowBrowsing) {
        this.allowBrowsing = allowBrowsing;
        return this;
    }

    public MaterialFileChooser minSelectedItems(int minSelectedItems) {
        this.minSelectedItems = Math.max(minSelectedItems, 0);
        return this;
    }

    public MaterialFileChooser allowCreateFolder(boolean allowCreateFolder) {
        this.allowCreateFolder = allowCreateFolder;
        mBotaoCriarPasta.setVisibility(allowCreateFolder ? View.VISIBLE : View.GONE);
        return this;
    }

    public MaterialFileChooser restoreFolder(boolean restoreFolder) {
        this.restoreFolder = restoreFolder;
        return this;
    }

    public MaterialFileChooser initialFolder(File initialFolder) {
        this.initialFolder = initialFolder;
        this.pastaAtual = initialFolder;
        //Limpa a pilha
        pilhaDeCaminhos.clear();
        //Insere na pilha.
        pilhaDeCaminhos.addFirst(pastaAtual);
        //Estado desta pasta.
        selecionarTudoStatus.put(pastaAtual, false);
        return this;
    }

    public MaterialFileChooser showFoldersFirst(boolean showFolderFirst) {
        this.showFoldersFirst = showFolderFirst;
        return this;
    }

    public MaterialFileChooser filter(Filter filter) {
        filters.add(filter);
        return this;
    }

    public MaterialFileChooser sorter(Sorter sorter) {
        if (sorter == null) sorter = Sorter.SORT_BY_NAME_ASC;
        this.ordenacao = sorter;
        return this;
    }

    private void populateBreadCrumbView(File file) {
        //Limpa
        mCaminhoDoDiretorio.getItens().clear();
        //Verifica se é um arquivo
        if (!FileHelper.isFolder(file)) {
            file = file.getParentFile();
        }
        //Obtém a pasta-pai.
        final File parent = file.getParentFile();
        //Não tem pasta-pai.
        if (parent == null) {
            FileBreadCrumItem item = new RootFileBreadCrumItem(file);
            mCaminhoDoDiretorio.addItem(item);
        }
        //Tem pasta-pai.
        else {
            populateBreadCrumbView(parent);
            FileBreadCrumItem item = new FileBreadCrumItem(file);
            mCaminhoDoDiretorio.addItem(item);
        }
    }

    private List<File> scanFiles(File file) {
        //Obtém a lista de arquivos.
        final List<File> fileList = FileHelper.listFiles(file, chooserFileFilter);
        //Não foi possível obter a lista de arquivos.
        //Seta a quantidade de itens exibidos.
        mQuantidadeDeItens.setText(String.valueOf(fileList.size()));
        //Orderná-la.
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return compareFile(a, b);
            }
        });
        return fileList;
    }

    //Compara dois arquivos.
    private int compareFile(File a, File b) {
        if (showFoldersFirst) {
            return a.isDirectory() == b.isDirectory() ?
                    ordenacao.compare(a, b) :
                    a.isDirectory() ? -1 : 1;
        } else {
            return a.isFile() == b.isFile() ?
                    ordenacao.compare(a, b) :
                    a.isFile() ? -1 : 1;
        }
    }

    private void populateRecyclerView(File file) {
        listaDeArquivosEPastasAdapter.setData(arquivosAtuais = scanFiles(file));
    }

    private void loadCurrentFolder() {
        if (pastaAtual != null) {
            populateBreadCrumbView(pastaAtual);
            populateRecyclerView(pastaAtual);
            mTamanhoTotal.setText(FileHelper.sizeAsString(pastaAtual, false));
        }
    }

    private boolean backTo(File file) {
        if (allowBrowsing && FileHelper.isFolder(file)) {
            pastaAtual = file;
            loadCurrentFolder();
            //Define o estado do botão selecionar tudo.
            mSelecionarTudo.setChecked(allowMultipleFiles && selecionarTudoStatus.get(file));
            return true;
        } else {
            return false;
        }
    }

    public boolean back() {
        //Se pode navegar e há pasta pra navegar.
        if (allowBrowsing && pilhaDeCaminhos.size() > 1) {
            //Remove a pasta atual da pilha.
            pilhaDeCaminhos.removeFirst();
            //Retorna para a pasta anterior.
            return backTo(pilhaDeCaminhos.getFirst());
        } else {
            return false;
        }
    }

    public void goTo(File file) {
        if (!allowBrowsing) {
            loadCurrentFolder();
        } else if (FileHelper.isFolder(file)) {
            pastaAtual = file;
            pilhaDeCaminhos.addFirst(pastaAtual);
            //Ainda não navegou nesta pasta. O selecionar tudo está desabilitado.
            if (allowMultipleFiles && !selecionarTudoStatus.containsKey(file)) {
                selecionarTudoStatus.put(file, false);
            }
            loadCurrentFolder();
            //Define o estado do botão selecionar tudo.
            mSelecionarTudo.setChecked(allowMultipleFiles && selecionarTudoStatus.get(file));
        }
    }

    public boolean goToPreviouslySelectedFolder() {
        //Busca uma pasta anteriormente selecionada.
        File folder = prefsManager.getPreviouslySelectedDiretory();
        //Há uma pasta.
        if (folder != null) {
            //Vá para esta pasta.
            goTo(folder);
            return true;
        } else {
            return false;
        }
    }

    public void goToStart() {
        goTo(initialFolder);
    }

    public void show() {
        //Não é pra restaurar e não há pasta pra restaurar.
        if (!restoreFolder || !goToPreviouslySelectedFolder()) {
            //Abrir direto na pasta inicial.
            goTo(initialFolder);
        }
        //Exibe o dialog.
        dialog = builder.show();
    }

    public interface OnFileChooserListener {

        void onItemSelected(List<File> files);

        void onCancelled();
    }

    //Item para a pasta raiz.
    private static class RootFileBreadCrumItem extends FileBreadCrumItem {

        public RootFileBreadCrumItem(File file) {
            super(file);
        }

        @Override
        public String getText() {
            return "/";
        }
    }

    //Item para uma pasta.
    private static class FileBreadCrumItem extends BreadCrumbItem<File> {

        public FileBreadCrumItem(File file) {
            setItens(Collections.singletonList(file));
        }

        @Override
        public String getText() {
            return getSelectedItem().getName();
        }
    }

    //Filtro de arquivos.
    private class ChooserFileFilter implements FileFilter {

        @Override
        public boolean accept(File f) {
            final boolean showHidden = showHiddenFiles || !f.isHidden();
            return  //Buscar
                    (TextUtils.isEmpty(busca) || f.getName().toLowerCase().contains(busca)) &&
                            //Exibir arquivos
                            ((showFiles && FileHelper.isFile(f) && showHidden) ||
                                    //Exibir pastas
                                    (showFolders && FileHelper.isFolder(f) && showHidden)) &&
                            //Filtros
                            filter(f);
        }

        private boolean filter(File f) {
            //Não há filtros.
            if (filters.size() == 0) return true;
            //Filtra.
            for (Filter filter : filters) {
                if (filter.accept(f)) {
                    return true;
                }
            }
            return false;
        }
    }

    //Constrói o MaterialDialog.
    private class Builder extends MaterialDialog.Builder {

        public Builder(@NonNull Context context, String title) {
            super(context);
            init(context);
            TextView mTitulo = customView.findViewById(R.id.titulo);
            if (title != null) {
                mTitulo.setText(title);
            } else {
                mTitulo.setVisibility(View.GONE);
            }
        }

        public Builder(@NonNull Context context, @StringRes int title) {
            super(context);
            init(context);
            TextView mTitulo = customView.findViewById(R.id.titulo);
            if (title != 0) {
                mTitulo.setText(title);
            } else {
                mTitulo.setVisibility(View.GONE);
            }
        }

        private void init(@NonNull Context context) {
            customView(R.layout.dialog_file_chooser, false);
            positiveText(android.R.string.ok);
            negativeText(android.R.string.cancel);
            //Views.
            TypedValue backgroundValue = new TypedValue();
            android.content.res.Resources.Theme theme = context.getTheme();
            //Cor de fundo do tema.
            theme.resolveAttribute(R.attr.mfc_theme_background_color, backgroundValue, true);
            backgroundColor(backgroundValue.data);
            //Cor de frente do tema.
            TypedValue foregroundValue = new TypedValue();
            theme.resolveAttribute(R.attr.mfc_theme_foreground_color, foregroundValue, true);
            positiveColor(foregroundValue.data);
            negativeColor(foregroundValue.data);
            cancelable(false);
            canceledOnTouchOutside(false);
            autoDismiss(false);

            mCaminhoDoDiretorio = customView.findViewById(R.id.caminhoDoDiretorio);
            mListaDeArquivosEPastas = customView.findViewById(R.id.listaDeArquivosEPastas);
            mListaDeArquivosEPastas.setLayoutManager(new LinearLayoutManager(context));
            listaDeArquivosEPastasAdapter.attachTo(mListaDeArquivosEPastas);
            mTamanhoTotal = customView.findViewById(R.id.tamanhoTotal);
            mQuantidadeDeItens = customView.findViewById(R.id.quantidadeDeItens);
            mBotaoVoltar = customView.findViewById(R.id.botaoVoltar);
            mIrParaDiretorioInicial = customView.findViewById(R.id.irParaDiretorioInicial);
            mQuantidadeDeItensSelecionados = customView.findViewById(R.id.quantidadeDeItensSelecionados);
            mBotaoBuscar = customView.findViewById(R.id.botaoBuscar);
            mCampoDeBusca = customView.findViewById(R.id.campoDeBusca);
            mCampoDeBuscaBox = customView.findViewById(R.id.campoDeBuscaBox);
            mSwipeRefreshLayout = customView.findViewById(R.id.swipeRefreshLayout);
            mSwipeRefreshLayout.setColorSchemeColors(foregroundValue.data);
            mSelecionarTudo = customView.findViewById(R.id.botaoSelecionarTudo);
            mBotaoCriarPasta = customView.findViewById(R.id.criarPasta);

            //Eventos.
            onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    if (arquivosSelecionados.size() >= minSelectedItems) {
                        if (fileChooserListener != null) {
                            //Cria a lista com os arquivos selecionados.
                            final List<File> files = new ArrayList<>(arquivosSelecionados.size());
                            files.addAll(arquivosSelecionados);
                            //Dispara o evento passando a lista.
                            fileChooserListener.onItemSelected(Collections.unmodifiableList(files));
                        }
                        dialog.dismiss();
                    }
                }
            });
            onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    if (arquivosSelecionados.size() >= minSelectedItems) {
                        //Dispara o evento.
                        if (fileChooserListener != null) {
                            fileChooserListener.onCancelled();
                        }
                        dialog.dismiss();
                    }
                }
            });
            dismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    prefsManager.setPreviouslySelectedDiretory(pastaAtual);
                }
            });
        }
    }
}
