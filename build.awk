/<!-- js.deps -->/ {
    split(ENVIRON["DEPS"], DEPS)
    # this way it goes from 1 to 9 instead of random ordering
    for (i = 1; DEPS[i]; i++)
        printf("<script type=\"text/javascript\" src=\"%s\"></script>\n", DEPS[i])
    next
}

1 # print everything
