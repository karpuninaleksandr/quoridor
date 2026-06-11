\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{csmathnotes}[2024/05/06]
\LoadClass[a5paper]{article}
\RequirePackage[backend=biber,style=gost-numeric,language=auto,autolang=other,sorting=none,movenames=false]{biblatex}
\RequirePackage{indentfirst}
\RequirePackage[colorlinks=true,urlcolor=blue,linkcolor=blue,citecolor=blue]{hyperref}
\RequirePackage[margin=2cm,bottom=2.5cm,headsep=3mm]{geometry}
\RequirePackage{fancyhdr}
\RequirePackage{graphicx}
\RequirePackage{enumitem}
\RequirePackage{url}
\RequirePackage{amsmath}
\RequirePackage{amssymb}
\RequirePackage{amsthm}
\RequirePackage{luacode}
\RequirePackage[labelsep=period]{caption}
\RequirePackage{microtype}
\RequirePackage{fontspec}
\RequirePackage[english,russian]{babel}
\RequirePackage{unicode-math}
\RequirePackage[labelsep=period]{caption}
\setmainfont{STIX Two Text}
\setmathfont{STIX Two Math}
\setsansfont{Roboto}[Scale=0.88]
\setmonofont{PT Mono}[Scale=0.94]

\pagestyle{fancy}
\lhead{\it \foreignlanguage{russian}{Заметки по информатике и математике}}
\chead{}
\rhead{\it \foreignlanguage{russian}{Выпуск 18}}
\lfoot{}
\cfoot{\thepage}
\rfoot{}
\renewcommand{\headrulewidth}{0.4pt}

\newtheorem{theorem}{Теорема}
\newtheorem{lemma}{Лемма}
\newtheorem{corollary}{Следствие}

\urlstyle{same}
\setlist{nosep}

\clubpenalty=10000 
\widowpenalty=10000

\let\oldbibliography\thebibliography
\renewcommand{\thebibliography}[1]{%
  \oldbibliography{#1}%
  \setlength{\itemsep}{-3pt}%
}
\renewcommand*{\newblockpunct}{\addperiod\space\bibsentence}

\let\footnoterule\relax

\newcommand{\keywords}[1]{\emph{Ключевые слова:} #1}

\directlua { require('metadata_proc.lua') }

\newcommand\udc[1]{\gdef\@udc{#1}}
\renewcommand{\author}[1]{\luaexec{add_author("#1")}}
\newcommand{\advisor}[2]{\luaexec{add_advisor("#1", "#2")}}
\newcommand{\affiliation}[1]{\luaexec{set_affiliation("#1")}}
\newcommand{\email}[1]{\luaexec{set_author_email("#1")}}
\newcommand{\advisoremail}[1]{}

\renewcommand\section{\@startsection {section}{1}{\z@}%
  {-2ex \@plus -1ex \@minus -.2ex}%
  {0.5ex \@plus.2ex}%
  {\normalfont\bfseries}}

\def\@maketitle{%
  \newpage
  \null
  \vspace*{-5mm}
  {\small\noindent УДК \@udc}
  \begin{center}%
    \let\thefootnote\relax%
    \footnote{\textcopyright\ \directlua{tex.print(output_authors_for_copyright())}, 2026}%
    \textbf{\@title}\par%
    \vskip 0.5em%
    \directlua{tex.print(output_authors())}\par
    {\small E-mail: \directlua{tex.print(output_authors_emails())}\par}
    {\small\directlua{tex.print(output_affiliations())}\par}
  \end{center}%
  \par
}
\def\@thanks{%
  \thispagestyle{fancy}
}

\ifdefined\firstpagenum
\setcounter{page}{\firstpagenum}
\fi
